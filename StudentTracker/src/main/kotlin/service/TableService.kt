package org.example.service

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.Permission
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import org.example.model.TableLink
import org.example.repository.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TableService(
    @Value("\${google.credentials.path}")
    private val credentialsResource: Resource,
    private val studentRepository: StudentRepository,
    private val scheduleRepository: ScheduleRepository,
    private val groupStreamRepository: GroupStreamRepository,
    private val tableLinkRepository: TableLinkRepository
) {
    companion object {
        private const val APPLICATION_NAME = "Student Attendance Tracker"
        private const val LESSONS_COUNT = 17
        private const val BASE_SHEETS_URL = "https://docs.google.com/spreadsheets/d/"
        private val logger = LoggerFactory.getLogger(TableService::class.java)
    }

    private val sheetsService: Sheets by lazy { initSheetsService() }
    private val driveService: Drive by lazy { initDriveService() }

    fun createAttendanceSheetsForAllStreams(): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        logger.info("Starting creation of attendance sheets for all streams")

        try {
            val streams = groupStreamRepository.findAllStreamNames()
            logger.info("Found ${streams.size} streams: $streams")

            streams.forEach { stream ->
                logger.info("Processing stream: $stream")
                processStream(stream, result)
            }
        } catch (e: Exception) {
            logger.error("Error while creating attendance sheets", e)
            throw TableGenerationException("Ошибка при создании таблиц: ${e.message}", e)
        }

        logger.info("Successfully created ${result.size} attendance sheets")
        return result
    }

    private fun processStream(stream: String, result: MutableMap<String, List<String>>) {
        logger.debug("Processing stream: $stream")

        val groupsInStream = groupStreamRepository.findGroupNamesByStreamName(stream)
        if (groupsInStream.isEmpty()) {
            logger.warn("No groups found for stream: $stream")
            return
        }
        logger.debug("Found groups for stream $stream: $groupsInStream")

        val subjects = scheduleRepository.findDistinctSubjectsByGroups(groupsInStream)
        if (subjects.isEmpty()) {
            logger.warn("No subjects found for groups: $groupsInStream")
            return
        }
        logger.debug("Found subjects for stream $stream: $subjects")

        subjects.forEach { subject ->
            try {
                val spreadsheetName = "$subject$stream"
                logger.debug("Processing subject: $subject for stream: $stream")

                val existingLink = tableLinkRepository.findByStreamNameAndSubject(stream, subject)
                if (existingLink != null) {
                    logger.info("Found existing table for $spreadsheetName, skipping creation")
                    result[spreadsheetName] = listOf(existingLink.link)
                    return@forEach
                }

                logger.info("Creating new spreadsheet for $spreadsheetName")
                val spreadsheetId = createNewSpreadsheet(subject, stream, groupsInStream)
                val url = "$BASE_SHEETS_URL$spreadsheetId"

                logger.debug("Saving table link for $spreadsheetName")
                saveTableLink(stream, subject, url)
                result[spreadsheetName] = listOf(url)
                logger.info("Successfully created spreadsheet: $url")
            } catch (e: Exception) {
                logger.error("Error processing subject $subject for stream $stream", e)
                throw TableGenerationException("Ошибка при обработке потока $stream и предмета $subject: ${e.message}", e)
            }
        }
    }

    private fun createNewSpreadsheet(subject: String, stream: String, groups: List<String>): String {
        logger.debug("Creating new spreadsheet for $subject$stream with groups: $groups")

        val spreadsheet = Spreadsheet()
            .setProperties(SpreadsheetProperties().setTitle("$subject$stream"))

        val createdSpreadsheet = try {
            sheetsService.spreadsheets().create(spreadsheet).execute()
        } catch (e: Exception) {
            logger.error("Failed to create spreadsheet", e)
            throw e
        }

        val spreadsheetId = createdSpreadsheet.spreadsheetId
        logger.debug("Created spreadsheet with ID: $spreadsheetId")

        try {
            logger.debug("Setting permissions for spreadsheet $spreadsheetId")
            setSpreadsheetPermissions(spreadsheetId)

            logger.debug("Setting up sheets for spreadsheet $spreadsheetId")
            setupSpreadsheetSheets(spreadsheetId, groups)
        } catch (e: Exception) {
            logger.error("Error setting up spreadsheet, attempting to delete", e)
            try {
                driveService.files().delete(spreadsheetId).execute()
                logger.warn("Deleted spreadsheet $spreadsheetId due to setup error")
            } catch (ignored: Exception) {
                logger.error("Failed to delete spreadsheet after setup error", ignored)
            }
            throw e
        }

        return spreadsheetId
    }

    private fun setupSpreadsheetSheets(spreadsheetId: String, groups: List<String>) {
        logger.debug("Setting up sheets for $spreadsheetId with groups: $groups")

        groups.forEachIndexed { index, group ->
            try {
                logger.debug("Processing group $group (index $index)")

                val students = getFormattedStudentNames(group)
                if (students.isEmpty()) {
                    logger.warn("No students found for group $group, skipping")
                    return@forEachIndexed
                }
                logger.debug("Found ${students.size} students for group $group")

                if (index > 0) {
                    logger.debug("Adding new sheet for group $group")
                    addNewSheet(spreadsheetId, group, index)
                } else {
                    logger.debug("Renaming first sheet to $group")
                    renameFirstSheet(spreadsheetId, group)
                }

                logger.debug("Filling sheet for group $group")
                fillSheet(spreadsheetId, index, students)
                logger.info("Successfully set up sheet for group $group")
            } catch (e: Exception) {
                logger.error("Error setting up sheet for group $group", e)
                throw TableGenerationException("Ошибка при настройке листа для группы $group: ${e.message}", e)
            }
        }
    }

    private fun saveTableLink(stream: String, subject: String, url: String) {
        logger.debug("Saving table link for stream $stream and subject $subject")
        tableLinkRepository.save(
            TableLink(
                streamName = stream,
                subject = subject,
                link = url,
                createdAt = LocalDateTime.now()
            )
        )
        logger.info("Saved table link: $url")
    }

    private fun getFormattedStudentNames(group: String): List<String> {
        logger.debug("Getting student names for group $group")
        return studentRepository.findByGroupStreamGroupName(group)
            .map {
                val name = "${it.surname} ${it.name} ${it.patronymic ?: ""}".trim()
                logger.trace("Student: $name")
                name
            }
    }

    private fun initSheetsService(): Sheets {
        logger.info("Initializing Google Sheets service")
        return try {
            val credentials = GoogleCredentials.fromStream(credentialsResource.inputStream)
                .createScoped(listOf(
                    "https://www.googleapis.com/auth/spreadsheets",
                    "https://www.googleapis.com/auth/drive"
                ))

            Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                HttpCredentialsAdapter(credentials)
            ).setApplicationName(APPLICATION_NAME)
                .build()
        } catch (e: Exception) {
            logger.error("Failed to initialize Sheets service", e)
            throw e
        }
    }

    private fun initDriveService(): Drive {
        logger.info("Initializing Google Drive service")
        return try {
            val credentials = GoogleCredentials.fromStream(credentialsResource.inputStream)
                .createScoped(listOf(
                    "https://www.googleapis.com/auth/drive",
                    "https://www.googleapis.com/auth/spreadsheets"
                ))

            Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME).build()
        } catch (e: Exception) {
            logger.error("Failed to initialize Drive service", e)
            throw e
        }
    }

    private fun setSpreadsheetPermissions(spreadsheetId: String) {
        logger.debug("Setting permissions for spreadsheet $spreadsheetId")
        try {
            driveService.permissions().create(spreadsheetId,
                Permission().setType("anyone").setRole("writer").setAllowFileDiscovery(false)
            ).execute()
            logger.info("Permissions set for spreadsheet $spreadsheetId")
        } catch (e: Exception) {
            logger.error("Failed to set permissions for spreadsheet $spreadsheetId", e)
            throw e
        }
    }

    private fun addNewSheet(spreadsheetId: String, groupName: String, sheetId: Int) {
        logger.debug("Adding new sheet '$groupName' with ID $sheetId to spreadsheet $spreadsheetId")
        try {
            sheetsService.spreadsheets().batchUpdate(
                spreadsheetId,
                BatchUpdateSpreadsheetRequest().setRequests(listOf(
                    Request().setAddSheet(
                        AddSheetRequest().setProperties(
                            SheetProperties()
                                .setTitle(groupName)
                                .setSheetId(sheetId)
                        )
                    )
                ))
            ).execute()
            logger.info("Added new sheet '$groupName' successfully")
        } catch (e: Exception) {
            logger.error("Failed to add new sheet '$groupName'", e)
            throw e
        }
    }

    private fun renameFirstSheet(spreadsheetId: String, groupName: String) {
        logger.debug("Renaming first sheet to '$groupName' in spreadsheet $spreadsheetId")
        try {
            sheetsService.spreadsheets().batchUpdate(spreadsheetId,
                BatchUpdateSpreadsheetRequest().setRequests(listOf(
                    Request().setUpdateSheetProperties(
                        UpdateSheetPropertiesRequest()
                            .setProperties(SheetProperties().setSheetId(0).setTitle(groupName))
                            .setFields("title")
                    )
                ))
            ).execute()
            logger.info("Renamed first sheet to '$groupName' successfully")
        } catch (e: Exception) {
            logger.error("Failed to rename first sheet to '$groupName'", e)
            throw e
        }
    }

    private fun fillSheet(spreadsheetId: String, sheetId: Int, studentNames: List<String>) {
        logger.debug("Filling sheet ID $sheetId with ${studentNames.size} students")
        try {
            val requests = mutableListOf<Request>().apply {
                add(setBaseFormattingRequest(sheetId, studentNames.size))
                add(createHeaderRequest(sheetId))
                add(createStudentNamesRequest(sheetId, studentNames))
                add(createAttendancePercentageRequest(sheetId, studentNames.size))
                add(createAttendanceCountRequest(sheetId, studentNames.size))
                add(createLessonSummaryRequest(sheetId, studentNames.size))
                add(createBordersRequest(sheetId, studentNames.size))
                addAll(createColumnSizingRequests(sheetId))
            }

            sheetsService.spreadsheets().batchUpdate(spreadsheetId,
                BatchUpdateSpreadsheetRequest().setRequests(requests)
            ).execute()
            logger.info("Successfully filled sheet ID $sheetId")
        } catch (e: Exception) {
            logger.error("Failed to fill sheet ID $sheetId", e)
            throw e
        }
    }

    private fun setBaseFormattingRequest(sheetId: Int, studentCount: Int): Request {
        return Request().setRepeatCell(
            RepeatCellRequest()
                .setRange(
                    GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(0)
                        .setEndRowIndex(studentCount + 2)
                        .setStartColumnIndex(0)
                        .setEndColumnIndex(LESSONS_COUNT + 3)
                )
                .setCell(
                    CellData()
                        .setUserEnteredFormat(
                            CellFormat()
                                .setTextFormat(
                                    TextFormat()
                                        .setFontFamily("Tahoma")
                                        .setFontSize(12)
                                )
                                .setHorizontalAlignment("CENTER")
                        )
                )
                .setFields("userEnteredFormat")
        )
    }

    private fun createHeaderRequest(sheetId: Int): Request {
        val headerValues = listOf(
            listOf("Неделя/Студент", *(1..LESSONS_COUNT).map { "$it" }.toTypedArray(), "%", "кол-во")
        )

        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(sheetId)
                        .setRowIndex(0)
                        .setColumnIndex(0)
                )
                .setRows(
                    listOf(
                        RowData().setValues(
                            headerValues[0].map { value ->
                                CellData()
                                    .setUserEnteredValue(ExtendedValue().setStringValue(value))
                                    .setUserEnteredFormat(
                                        CellFormat()
                                            .setTextFormat(TextFormat().setBold(true))
                                    )
                            }
                        )
                    )
                )
                .setFields("userEnteredValue,userEnteredFormat.textFormat.bold")
        )
    }

    private fun createStudentNamesRequest(sheetId: Int, studentNames: List<String>): Request {
        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(sheetId)
                        .setRowIndex(1)
                        .setColumnIndex(0)
                )
                .setRows(
                    studentNames.map { name ->
                        RowData().setValues(
                            listOf(
                                CellData().setUserEnteredValue(
                                    ExtendedValue().setStringValue(name)
                                )
                            )
                        )
                    }
                )
                .setFields("userEnteredValue")
        )
    }

    private fun createAttendancePercentageRequest(sheetId: Int, studentCount: Int): Request {
        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(sheetId)
                        .setRowIndex(1)
                        .setColumnIndex(LESSONS_COUNT + 1)
                )
                .setRows(
                    (1..studentCount).map { row ->
                        val range = "B${row + 1}:${('A' + LESSONS_COUNT)}$row}"
                        RowData().setValues(
                            listOf(
                                CellData().setUserEnteredValue(
                                    ExtendedValue().setFormulaValue(
                                        "=ROUND(100*(COUNTIF($range,\"1\")/$LESSONS_COUNT), 1) & \"%\""
                                    )
                                )
                            )
                        )
                    }
                )
                .setFields("userEnteredValue")
        )
    }

    private fun createAttendanceCountRequest(sheetId: Int, studentCount: Int): Request {
        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(sheetId)
                        .setRowIndex(1)
                        .setColumnIndex(LESSONS_COUNT + 2)
                )
                .setRows(
                    (1..studentCount).map { row ->
                        val range = "B${row + 1}:${('A' + LESSONS_COUNT)}$row}"
                        RowData().setValues(
                            listOf(
                                CellData().setUserEnteredValue(
                                    ExtendedValue().setFormulaValue(
                                        "=COUNTIF($range,\"1\")"
                                    )
                                )
                            )
                        )
                    }
                )
                .setFields("userEnteredValue")
        )
    }

    private fun createLessonSummaryRequest(sheetId: Int, studentCount: Int): Request {
        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(sheetId)
                        .setRowIndex(studentCount + 1)
                        .setColumnIndex(0)
                )
                .setRows(
                    listOf(
                        RowData().setValues(
                            listOf(
                                CellData().setUserEnteredValue(
                                    ExtendedValue().setStringValue("Итого на паре:")
                                ),
                                *(1..LESSONS_COUNT).map { lessonCol ->
                                    val colLetter = ('A' + lessonCol).toChar()
                                    CellData().setUserEnteredValue(
                                        ExtendedValue().setFormulaValue(
                                            "=COUNTIF(${colLetter}2:${colLetter}${studentCount + 1},\"1\")"
                                        )
                                    )
                                }.toTypedArray(),
                                CellData(),
                                CellData()
                            )
                        )
                    )
                )
                .setFields("userEnteredValue")
        )
    }

    private fun createBordersRequest(sheetId: Int, studentCount: Int): Request {
        return Request().setUpdateBorders(
            UpdateBordersRequest()
                .setRange(
                    GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(0)
                        .setEndRowIndex(studentCount + 2)
                        .setStartColumnIndex(0)
                        .setEndColumnIndex(LESSONS_COUNT + 3)
                )
                .setTop(Border().setStyle("SOLID").setWidth(1))
                .setBottom(Border().setStyle("SOLID").setWidth(1))
                .setLeft(Border().setStyle("SOLID").setWidth(1))
                .setRight(Border().setStyle("SOLID").setWidth(1))
                .setInnerHorizontal(Border().setStyle("SOLID").setWidth(1))
                .setInnerVertical(Border().setStyle("SOLID").setWidth(1))
        )
    }

    private fun createColumnSizingRequests(sheetId: Int): List<Request> {
        return listOf(
            Request().setUpdateDimensionProperties(
                UpdateDimensionPropertiesRequest()
                    .setRange(
                        DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("COLUMNS")
                            .setStartIndex(0)
                            .setEndIndex(1)
                    )
                    .setProperties(
                        DimensionProperties()
                            .setPixelSize(200)
                    )
                    .setFields("pixelSize")
            ),
            Request().setUpdateDimensionProperties(
                UpdateDimensionPropertiesRequest()
                    .setRange(
                        DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("COLUMNS")
                            .setStartIndex(1)
                            .setEndIndex(LESSONS_COUNT + 3)
                    )
                    .setProperties(
                        DimensionProperties()
                            .setPixelSize(80)
                    )
                    .setFields("pixelSize")
            )
        )
    }
}

class TableGenerationException(message: String, cause: Throwable?) :
    RuntimeException(message, cause)