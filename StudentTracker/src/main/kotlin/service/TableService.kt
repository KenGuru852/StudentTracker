package org.example.service

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.Permission
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import org.example.model.GroupStream
import org.example.model.Schedule
import org.example.model.Student
import org.example.model.TableLink
import org.example.repository.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheConfig
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@CacheConfig(cacheNames = ["studentsCache"])
class TableService(
    @Value("\${google.credentials.path}")
    private val credentialsResource: Resource,
    private val studentRepository: StudentRepository,
    private val scheduleRepository: ScheduleRepository,
    private val groupStreamRepository: GroupStreamRepository,
    private val tableLinkRepository: TableLinkRepository,
    private val teacherRepository: TeacherRepository,
    private val studentService: StudentService
) {
    companion object {
        private const val APPLICATION_NAME = "Student Attendance Tracker"
        private const val LESSONS_COUNT = 17
        private const val BASE_SHEETS_URL = "https://docs.google.com/spreadsheets/d/"
        private const val API_DELAY_MS = 1500L
        private val logger = LoggerFactory.getLogger(TableService::class.java)
    }

    private val sheetsService: Sheets by lazy { initSheetsService() }
    private val driveService: Drive by lazy { initDriveService() }

    fun createAttendanceSheetsForAllStreams(): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        logger.info("Starting creation of attendance sheets for all streams")

        try {
            // Получаем все данные одним запросом
            val allStreams = groupStreamRepository.findAll()
            val allGroups = allStreams.map { it.groupName }
            val allSchedules = scheduleRepository.findByGroupNameIn(allGroups)
            val allStudents = studentRepository.findAll()
            val existingLinks = tableLinkRepository.findAll()

            // Группируем данные для быстрого доступа
            val streamsMap = allStreams.groupBy { it.streamName }
            val schedulesMap = allSchedules.groupBy { it.groupName }
            val studentsMap = allStudents.groupBy { it.groupStream.groupName }
            val existingLinksMap = existingLinks.groupBy { "${it.subject}${it.streamName}" }

            streamsMap.keys.forEachIndexed { index, stream ->
                logger.info("Processing stream: $stream")
                processStreamOptimized(stream, streamsMap, schedulesMap, studentsMap, existingLinksMap, result)
            }

            logger.info("Successfully created ${result.size} spreadsheets")
        } catch (e: Exception) {
            logger.error("Error creating tables: ${e.message}", e)
            throw TableGenerationException("Ошибка при создании таблиц: ${e.message}", e)
        }

        return result
    }

    private fun processStreamOptimized(
        stream: String,
        streamsMap: Map<String, List<GroupStream>>,
        schedulesMap: Map<String, List<Schedule>>,
        studentsMap: Map<String, List<Student>>,
        existingLinksMap: Map<String, List<TableLink>>,
        result: MutableMap<String, List<String>>
    ) {
        try {
            val groupsInStream = streamsMap[stream]?.map { it.groupName } ?: emptyList()
            if (groupsInStream.isEmpty()) {
                logger.warn("No groups found for stream $stream")
                return
            }

            val subjects = groupsInStream.flatMap {
                schedulesMap[it]?.map { it.subject } ?: emptyList()
            }.distinct()

            if (subjects.isEmpty()) {
                logger.warn("No subjects found for groups in stream $stream")
                return
            }

            subjects.forEachIndexed { index, subject ->
                try {
                    logger.info("Processing subject $subject for stream $stream")
                    val spreadsheetName = "$subject$stream"
                    val existingLinks = existingLinksMap[spreadsheetName]

                    if (!existingLinks.isNullOrEmpty()) {
                        logger.info("Spreadsheet already exists for $spreadsheetName, using existing link")
                        result[spreadsheetName] = existingLinks.map { it.link }
                        return@forEachIndexed
                    }

                    logger.info("Creating new spreadsheet for $spreadsheetName")
                    val spreadsheetId = createNewSpreadsheetOptimized(subject, stream, groupsInStream, studentsMap)
                    val url = "$BASE_SHEETS_URL$spreadsheetId"

                    // Получаем имя преподавателя для этого предмета
                    val teacherName = schedulesMap[groupsInStream.first()]?.first { it.subject == subject }?.teacher?.fullName
                        ?: "Неизвестный преподаватель"

                    saveTableLink(stream, subject, teacherName, url)
                    result[spreadsheetName] = listOf(url)
                    logger.info("Successfully created spreadsheet for $spreadsheetName with URL: $url")
                } catch (e: Exception) {
                    logger.error("Error processing subject $subject for stream $stream: ${e.message}", e)
                    throw TableGenerationException("Ошибка при обработке потока $stream и предмета $subject: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing stream $stream: ${e.message}", e)
            throw e
        }
    }

    private fun createNewSpreadsheetOptimized(
        subject: String,
        stream: String,
        groups: List<String>,
        studentsMap: Map<String, List<Student>>
    ): String {
        logger.debug("Creating new spreadsheet for subject $subject and stream $stream")

        // Получаем emails всех преподавателей, связанных с этим потоком
        val teacherEmails = groups.flatMap { group ->
            scheduleRepository.findByGroupName(group).map { it.teacher.email }
        }.toSet()

        val spreadsheet = Spreadsheet()
            .setProperties(SpreadsheetProperties().setTitle("$subject$stream"))

        val createdSpreadsheet = sheetsService.spreadsheets().create(spreadsheet).execute()
        val spreadsheetId = createdSpreadsheet.spreadsheetId
        logger.debug("Created spreadsheet with ID: $spreadsheetId")

        try {
            setSpreadsheetPermissions(spreadsheetId, teacherEmails)
            setupSpreadsheetSheetsOptimized(spreadsheetId, groups, studentsMap)
            logger.debug("Successfully configured spreadsheet $spreadsheetId")
        } catch (e: Exception) {
            logger.error("Error setting up spreadsheet, attempting to delete...", e)
            try {
                driveService.files().delete(spreadsheetId).execute()
                logger.warn("Deleted spreadsheet $spreadsheetId due to setup error")
            } catch (ignored: Exception) {
                logger.error("Failed to delete spreadsheet $spreadsheetId", ignored)
            }
            throw e
        }

        return spreadsheetId
    }

    private fun setupSpreadsheetSheetsOptimized(
        spreadsheetId: String,
        groups: List<String>,
        studentsMap: Map<String, List<Student>>
    ) {
        logger.debug("Setting up sheets for spreadsheet $spreadsheetId with groups: $groups")

        val batchRequests = mutableListOf<Request>()

        groups.forEachIndexed { index, group ->
            try {
                logger.debug("Processing group $group (index $index)")

                val students = studentsMap[group]?.map {
                    "${it.surname} ${it.name} ${it.patronymic ?: ""}".trim()
                } ?: emptyList()

                if (students.isEmpty()) {
                    logger.warn("No students found for group $group, skipping sheet creation")
                    return@forEachIndexed
                }

                if (index > 0) {
                    logger.debug("Adding new sheet for group $group")
                    batchRequests.add(addNewSheetRequest(group, index))
                } else {
                    logger.debug("Renaming first sheet for group $group")
                    batchRequests.add(renameFirstSheetRequest(group))
                }
            } catch (e: Exception) {
                logger.error("Error setting up sheet for group $group: ${e.message}", e)
                throw TableGenerationException("Ошибка при настройке листа для группы $group: ${e.message}", e)
            }
        }

        if (batchRequests.isNotEmpty()) {
            sheetsService.spreadsheets().batchUpdate(
                spreadsheetId,
                BatchUpdateSpreadsheetRequest().setRequests(batchRequests)
            ).execute()
        }

        groups.forEachIndexed { index, group ->
            val students = studentsMap[group]?.map {
                "${it.surname} ${it.name} ${it.patronymic ?: ""}".trim()
            }
            if (!students.isNullOrEmpty()) {
                fillSheet(spreadsheetId, index, students)
            }
        }
    }

    // Остальные методы остаются без изменений
    private fun saveTableLink(stream: String, subject: String, teacherName: String, url: String) {
        logger.debug("Saving table link for stream $stream, subject $subject and teacher $teacherName")
        tableLinkRepository.save(
            TableLink(
                streamName = stream,
                subject = subject,
                teacherName = teacherName,
                link = url,
                createdAt = LocalDateTime.now()
            )
        )
    }

    private fun initSheetsService(): Sheets {
        logger.debug("Initializing Sheets service")
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
                .also { logger.debug("Sheets service initialized successfully") }
        } catch (e: Exception) {
            logger.error("Error initializing Sheets service", e)
            throw e
        }
    }

    private fun initDriveService(): Drive {
        logger.debug("Initializing Drive service")
        val credentials = GoogleCredentials.fromStream(credentialsResource.inputStream)
            .createScoped(listOf(
                "https://www.googleapis.com/auth/drive",
                "https://www.googleapis.com/auth/spreadsheets"
            ))

        return Drive.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials))
            .setApplicationName(APPLICATION_NAME)
            .build()
            .also { logger.debug("Drive service initialized successfully") }
    }

    private fun setSpreadsheetPermissions(spreadsheetId: String, teacherEmails: Set<String>) {
        logger.debug("Setting permissions for spreadsheet $spreadsheetId")

        // Даем доступ на редактирование преподавателям
        teacherEmails.forEach { email ->
            driveService.permissions().create(spreadsheetId,
                Permission()
                    .setType("user")
                    .setRole("writer")
                    .setEmailAddress(email)
            ).execute()
        }

        // Даем доступ на чтение всем (студентам)
        driveService.permissions().create(spreadsheetId,
            Permission()
                .setType("anyone")
                .setRole("reader")
                .setAllowFileDiscovery(false)
        ).execute()
    }

    private fun addNewSheetRequest(groupName: String, sheetId: Int): Request {
        return Request().setAddSheet(
            AddSheetRequest().setProperties(
                SheetProperties()
                    .setTitle(groupName)
                    .setSheetId(sheetId)
            )
        )
    }

    private fun renameFirstSheetRequest(groupName: String): Request {
        return Request().setUpdateSheetProperties(
            UpdateSheetPropertiesRequest()
                .setProperties(SheetProperties().setSheetId(0).setTitle(groupName))
                .setFields("title")
        )
    }

    private fun fillSheet(spreadsheetId: String, sheetId: Int, studentNames: List<String>) {
        logger.debug("Filling sheet $sheetId with ${studentNames.size} students")
        val requests = mutableListOf<Request>().apply {
            add(setBaseFormattingRequest(sheetId, studentNames.size))
            add(createHeaderRequest(sheetId))
            add(createStudentNumbersRequest(sheetId, studentNames.size))
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
                        .setEndColumnIndex(LESSONS_COUNT + 4)
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
            listOf("№", "ФИО", *(1..LESSONS_COUNT).map { "$it" }.toTypedArray(), "%", "кол-во")
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

    private fun createStudentNumbersRequest(sheetId: Int, studentCount: Int): Request {
        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(sheetId)
                        .setRowIndex(1)
                        .setColumnIndex(0)
                )
                .setRows(
                    (1..studentCount).map { number ->
                        RowData().setValues(
                            listOf(
                                CellData().setUserEnteredValue(
                                    ExtendedValue().setNumberValue(number.toDouble()))
                            )
                        )
                    }
                )
                .setFields("userEnteredValue")
        )
    }

    private fun createStudentNamesRequest(sheetId: Int, studentNames: List<String>): Request {
        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(sheetId)
                        .setRowIndex(1)
                        .setColumnIndex(1)
                )
                .setRows(
                    studentNames.map { name ->
                        RowData().setValues(
                            listOf(
                                CellData().setUserEnteredValue(
                                    ExtendedValue().setStringValue(name))
                                    .setUserEnteredFormat(
                                        CellFormat()
                                            .setHorizontalAlignment("LEFT")
                                            .setWrapStrategy("WRAP"))
                            )
                        )
                    }
                )
                .setFields("userEnteredValue,userEnteredFormat.horizontalAlignment,userEnteredFormat.wrapStrategy")
        )
    }

    private fun createAttendancePercentageRequest(sheetId: Int, studentCount: Int): Request {
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
                        val startColumn = 'C'.code
                        val endColumn = startColumn + LESSONS_COUNT - 1
                        val range = "C${row + 1}:${endColumn.toChar()}${row + 1}"
                        RowData().setValues(
                            listOf(
                                CellData().setUserEnteredValue(
                                    ExtendedValue().setFormulaValue(
                                        "=ROUND(100 * (COUNTIF($range, \"1\") / $LESSONS_COUNT), 1) & \"%\""
                                    )
                                ).setUserEnteredFormat(
                                    CellFormat().setTextFormat(TextFormat().setBold(true))
                                )
                            )
                        )
                    }
                )
                .setFields("userEnteredValue,userEnteredFormat.textFormat.bold")
        )
    }

    private fun createAttendanceCountRequest(sheetId: Int, studentCount: Int): Request {
        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(sheetId)
                        .setRowIndex(1)
                        .setColumnIndex(LESSONS_COUNT + 3)
                )
                .setRows(
                    (1..studentCount).map { row ->
                        val range = "C${row + 1}:${('C'.toInt() + LESSONS_COUNT - 1).toChar()}${row + 1}"
                        RowData().setValues(
                            listOf(
                                CellData().setUserEnteredValue(
                                    ExtendedValue().setFormulaValue(
                                        "=COUNTIF($range, \"1\")"
                                    )
                                ).setUserEnteredFormat(
                                    CellFormat().setTextFormat(TextFormat().setBold(true))
                                )
                            )
                        )
                    }
                )
                .setFields("userEnteredValue,userEnteredFormat.textFormat.bold")
        )
    }

    private fun createLessonSummaryRequest(sheetId: Int, studentCount: Int): Request {
        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(sheetId)
                        .setRowIndex(studentCount + 1)
                        .setColumnIndex(1)
                )
                .setRows(
                    listOf(
                        RowData().setValues(
                            listOf(
                                CellData().setUserEnteredValue(
                                    ExtendedValue().setStringValue("Итого:")
                                ).setUserEnteredFormat(
                                    CellFormat().setTextFormat(TextFormat().setBold(true))
                                ),
                                *(1..LESSONS_COUNT).map { lessonCol ->
                                    val colLetter = ('A' + lessonCol + 1).toChar()
                                    CellData().setUserEnteredValue(
                                        ExtendedValue().setFormulaValue(
                                            "=COUNTIF(${colLetter}2:${colLetter}${studentCount + 1},\"1\")"
                                        )
                                    ).setUserEnteredFormat(
                                        CellFormat().setTextFormat(TextFormat().setBold(true))
                                    )
                                }.toTypedArray(),
                                CellData().setUserEnteredFormat(
                                    CellFormat().setTextFormat(TextFormat().setBold(true))
                                ),
                                CellData().setUserEnteredFormat(
                                    CellFormat().setTextFormat(TextFormat().setBold(true))
                                )
                            )
                        )
                    )
                )
                .setFields("userEnteredValue,userEnteredFormat.textFormat.bold")
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
                        .setEndColumnIndex(LESSONS_COUNT + 4)
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
                            .setPixelSize(50)
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
                            .setEndIndex(2)
                    )
                    .setProperties(
                        DimensionProperties()
                            .setPixelSize(300)
                    )
                    .setFields("pixelSize")
            ),
            Request().setUpdateDimensionProperties(
                UpdateDimensionPropertiesRequest()
                    .setRange(
                        DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("COLUMNS")
                            .setStartIndex(2)
                            .setEndIndex(LESSONS_COUNT + 4)
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