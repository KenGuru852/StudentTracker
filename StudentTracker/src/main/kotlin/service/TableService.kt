package org.example.service

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.Permission
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import java.io.FileInputStream

class AttendanceSheetService {
    companion object {
        private const val CREDENTIALS_PATH = "src/main/resources/credentials.json"
        private const val APPLICATION_NAME = "Student Attendance Tracker"
        private const val LESSONS_COUNT = 17
    }

    private val sheetsService: Sheets by lazy {
        val credentials = GoogleCredentials.fromStream(FileInputStream(CREDENTIALS_PATH))
            .createScoped(listOf(
                "https://www.googleapis.com/auth/spreadsheets",
                "https://www.googleapis.com/auth/drive"
            ))

        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        Sheets.Builder(httpTransport, GsonFactory.getDefaultInstance(), HttpCredentialsAdapter(credentials))
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    private val driveService: Drive by lazy {
        val credentials = GoogleCredentials.fromStream(FileInputStream(CREDENTIALS_PATH))
            .createScoped(listOf(
                "https://www.googleapis.com/auth/drive",
                "https://www.googleapis.com/auth/spreadsheets"
            ))

        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        Drive.Builder(httpTransport, GsonFactory.getDefaultInstance(), HttpCredentialsAdapter(credentials))
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    fun createAttendanceSheet(studentNames: List<String>): String {
        require(studentNames.isNotEmpty()) { "Student list cannot be empty" }

        val spreadsheet = Spreadsheet()
            .setProperties(SpreadsheetProperties().setTitle("Student Attendance - ${java.time.LocalDate.now()}"))

        val createdSpreadsheet = sheetsService.spreadsheets().create(spreadsheet).execute()
        val spreadsheetId = createdSpreadsheet.spreadsheetId

        val permission = Permission()
            .setType("anyone")
            .setRole("writer")
            .setAllowFileDiscovery(false)

        driveService.permissions().create(spreadsheetId, permission).execute()

        val requests = mutableListOf<Request>()

        requests.add(createDefaultFormatRequest())

        requests.add(createHeaderRequest())

        requests.add(createStudentNamesRequest(studentNames))

        requests.add(createBordersRequest(studentNames.size))

        requests.add(createAttendancePercentageRequest(studentNames.size))

        requests.add(createAttendanceCountRequest(studentNames.size))

        requests.add(createLessonSummaryRequest(studentNames.size))

        requests.addAll(createFormattingRequests(studentNames.size))

        requests.addAll(createColumnSizingRequests())

        val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(requests)
        sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()

        return "https://docs.google.com/spreadsheets/d/$spreadsheetId"
    }

    private fun createDefaultFormatRequest(): Request {
        return Request().setRepeatCell(
            RepeatCellRequest()
                .setRange(
                    GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(0)
                        .setEndRowIndex(1000)
                        .setStartColumnIndex(0)
                        .setEndColumnIndex(1000)
                )
                .setCell(
                    CellData()
                        .setUserEnteredFormat(
                            CellFormat()
                                .setTextFormat(
                                    TextFormat()
                                        .setFontFamily("Tahoma")
                                        .setFontSize(18)
                                )
                                .setHorizontalAlignment("CENTER")
                        )
                )
                .setFields("userEnteredFormat.textFormat.fontFamily,userEnteredFormat.textFormat.fontSize,userEnteredFormat.horizontalAlignment")
        )
    }

    private fun createHeaderRequest(): Request {
        val headerValues = listOf(
            listOf("Неделя/Студент", *(1..LESSONS_COUNT).map { "$it" }.toTypedArray(), "%", "кол-во")
        )

        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(0)
                        .setRowIndex(0)
                        .setColumnIndex(0)
                )
                .setRows(listOf(
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
                ))
                .setFields("userEnteredValue,userEnteredFormat.textFormat.bold")
        )
    }

    private fun createStudentNamesRequest(studentNames: List<String>): Request {
        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(0)
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

    private fun createBordersRequest(studentCount: Int): Request {
        return Request().setUpdateBorders(
            UpdateBordersRequest()
                .setRange(
                    GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(0)
                        .setEndRowIndex(studentCount + 2)
                        .setStartColumnIndex(0)
                        .setEndColumnIndex(LESSONS_COUNT + 3)
                )
                .setTop(Border().setStyle("SOLID").setColor(Color().setRed(0f).setGreen(0f).setBlue(0f)))
                .setBottom(Border().setStyle("SOLID").setColor(Color().setRed(0f).setGreen(0f).setBlue(0f)))
                .setLeft(Border().setStyle("SOLID").setColor(Color().setRed(0f).setGreen(0f).setBlue(0f)))
                .setRight(Border().setStyle("SOLID").setColor(Color().setRed(0f).setGreen(0f).setBlue(0f)))
                .setInnerHorizontal(Border().setStyle("SOLID").setColor(Color().setRed(0f).setGreen(0f).setBlue(0f)))
                .setInnerVertical(Border().setStyle("SOLID").setColor(Color().setRed(0f).setGreen(0f).setBlue(0f)))
        )
    }

    private fun createAttendancePercentageRequest(studentCount: Int): Request {
        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(0)
                        .setRowIndex(1)
                        .setColumnIndex(LESSONS_COUNT + 1)
                )
                .setRows(
                    (2..studentCount + 1).map { row ->
                        val range = "B$row:${('A' + LESSONS_COUNT).toChar()}$row"
                        RowData().setValues(
                            listOf(
                                CellData().setUserEnteredValue(
                                    ExtendedValue().setFormulaValue(
                                        "=ROUND(100*(COUNTIF($range,1)/$LESSONS_COUNT), 1) & \"%\""
                                    )
                                )
                            )
                        )
                    }
                )
                .setFields("userEnteredValue")
        )
    }

    private fun createAttendanceCountRequest(studentCount: Int): Request {
        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(0)
                        .setRowIndex(1)
                        .setColumnIndex(LESSONS_COUNT + 2)
                )
                .setRows(
                    (2..studentCount + 1).map { row ->
                        val range = "B$row:${('A' + LESSONS_COUNT).toChar()}$row"
                        RowData().setValues(
                            listOf(
                                CellData().setUserEnteredValue(
                                    ExtendedValue().setFormulaValue(
                                        "=COUNTIF($range,1)"
                                    )
                                )
                            )
                        )
                    }
                )
                .setFields("userEnteredValue")
        )
    }

    private fun createLessonSummaryRequest(studentCount: Int): Request {
        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(0)
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
                                            "=COUNTIF(${colLetter}2:${colLetter}${studentCount + 1},1)"
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

    private fun createFormattingRequests(studentCount: Int): List<Request> {
        return listOf(
            Request().setRepeatCell(
                RepeatCellRequest()
                    .setRange(
                        GridRange()
                            .setSheetId(0)
                            .setStartRowIndex(1)
                            .setEndRowIndex(studentCount + 1)
                            .setStartColumnIndex(0)
                            .setEndColumnIndex(1)
                    )
                    .setCell(
                        CellData()
                            .setUserEnteredFormat(
                                CellFormat()
                                    .setHorizontalAlignment("LEFT")
                            )
                    )
                    .setFields("userEnteredFormat.horizontalAlignment")
            ),

            Request().setRepeatCell(
                RepeatCellRequest()
                    .setRange(
                        GridRange()
                            .setSheetId(0)
                            .setStartRowIndex(0)
                            .setEndRowIndex(1)
                            .setStartColumnIndex(0)
                            .setEndColumnIndex(LESSONS_COUNT + 3)
                    )
                    .setCell(
                        CellData()
                            .setUserEnteredFormat(
                                CellFormat()
                                    .setHorizontalAlignment("CENTER")
                            )
                    )
                    .setFields("userEnteredFormat.horizontalAlignment")
            ),

            Request().setRepeatCell(
                RepeatCellRequest()
                    .setRange(
                        GridRange()
                            .setSheetId(0)
                            .setStartRowIndex(studentCount + 1)
                            .setEndRowIndex(studentCount + 2)
                            .setStartColumnIndex(0)
                            .setEndColumnIndex(LESSONS_COUNT + 3)
                    )
                    .setCell(
                        CellData()
                            .setUserEnteredFormat(
                                CellFormat()
                                    .setTextFormat(TextFormat().setBold(true))
                            )
                    )
                    .setFields("userEnteredFormat.textFormat.bold")
            )
        )
    }

    private fun createColumnSizingRequests(): List<Request> {
        return listOf(
            Request().setAutoResizeDimensions(
                AutoResizeDimensionsRequest()
                    .setDimensions(
                        DimensionRange()
                            .setSheetId(0)
                            .setDimension("COLUMNS")
                            .setStartIndex(0)
                            .setEndIndex(1)
                    )
            ),

            Request().setUpdateDimensionProperties(
                UpdateDimensionPropertiesRequest()
                    .setRange(
                        DimensionRange()
                            .setSheetId(0)
                            .setDimension("COLUMNS")
                            .setStartIndex(1)
                            .setEndIndex(LESSONS_COUNT + 3)
                    )
                    .setProperties(
                        DimensionProperties()
                            .setPixelSize(100)
                    )
                    .setFields("pixelSize")
            )
        )
    }
}

fun main() {
    val service = AttendanceSheetService()

    val students = listOf(
        "Иванов Иван Иванович",
        "Петров Петр Петрович",
        "Сидорова Анна Михайловна",
        "Кузнецов Алексей Сергеевич",
        "Смирнова Мария Владимировна",
        "Попов Дмитрий Александрович",
        "Васильева Ольга Николаевна",
        "Морозов Сергей Викторович",
        "Федорова Екатерина Андреевна",
        "Соловьев Николай Павлович",
        "Зайцева Татьяна Юрьевна",
        "Ковалев Артем Сергеевич",
        "Лебедева Светлана Игоревна",
        "Григорьев Андрей Валерьевич",
        "Тихонов Денис Олегович",
        "Климова Наталья Сергеевна",
        "Семенов Илья Дмитриевич",
        "Борисова Анастасия Викторовна",
        "Сергеев Владислав Юрьевич",
        "Михайлова Дарья Александровна",
        "Кузьмина Виктория Анатольевна",
        "Егоров Роман Валентинович",
        "Савельев Арсений Ильич",
        "Никитина Полина Сергеевна",
        "Фролов Константин Дмитриевич"
    )

    val sheetUrl = service.createAttendanceSheet(students)
    println("Таблица посещаемости создана: $sheetUrl")
}