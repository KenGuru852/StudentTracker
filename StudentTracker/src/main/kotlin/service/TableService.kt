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
        require(studentNames.size <= 30) { "Maximum 30 students allowed" }

        // 1. Создаем новую таблицу
        val spreadsheet = Spreadsheet()
            .setProperties(SpreadsheetProperties().setTitle("Student Attendance - ${java.time.LocalDate.now()}"))

        val createdSpreadsheet = sheetsService.spreadsheets().create(spreadsheet).execute()
        val spreadsheetId = createdSpreadsheet.spreadsheetId

        // 2. Настраиваем общий доступ (доступ для всех с ссылкой)
        val permission = Permission()
            .setType("anyone")
            .setRole("writer") // "reader" если нужно только чтение
            .setAllowFileDiscovery(false)

        driveService.permissions().create(spreadsheetId, permission).execute()

        // 3. Подготовка запросов для batchUpdate
        val requests = mutableListOf<Request>()

        // 3.1. Установка шрифта Tahoma 18 для всей таблицы
        requests.add(createFontRequest())

        // 3.2. Заполнение заголовков (C6-U6)
        requests.add(createHeaderRequest())

        // 3.3. Заполнение ФИО студентов (C7-C36)
        requests.add(createStudentNamesRequest(studentNames))

        // 3.4. Установка границ
        requests.add(createBordersRequest())

        // 3.5. Формулы для процента посещаемости
        requests.add(createAttendancePercentageRequest(studentNames.size))

        // 3.6. Форматирование
        requests.addAll(createFormattingRequests())

        // 4. Выполняем все запросы
        val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(requests)
        sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()

        return "https://docs.google.com/spreadsheets/d/$spreadsheetId"
    }

    private fun createFontRequest(): Request {
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
                        )
                )
                .setFields("userEnteredFormat.textFormat.fontFamily,userEnteredFormat.textFormat.fontSize")
        )
    }

    private fun createHeaderRequest(): Request {
        val headerValues = listOf(
            listOf("", *(1..17).map { it.toString() }.toTypedArray(), "")
        )

        return Request().setUpdateCells(
            UpdateCellsRequest()
                .setStart(
                    GridCoordinate()
                        .setSheetId(0)
                        .setRowIndex(5) // 6-я строка (0-based)
                        .setColumnIndex(2) // C столбец (0-based)
                )
                .setRows(listOf(
                    RowData().setValues(
                        headerValues[0].map { value ->
                            CellData()
                                .setUserEnteredValue(ExtendedValue().setStringValue(value))
                                .setUserEnteredFormat(
                                    CellFormat()
                                        .setTextFormat(
                                            TextFormat()
                                                .setBold(true)
                                        )
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
                        .setRowIndex(6) // 7-я строка
                        .setColumnIndex(2) // C столбец
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

    private fun createBordersRequest(): Request {
        return Request().setUpdateBorders(
            UpdateBordersRequest()
                .setRange(
                    GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(5) // 6-я строка
                        .setEndRowIndex(36) // 36-я строка
                        .setStartColumnIndex(2) // C столбец
                        .setEndColumnIndex(21) // U столбец
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
                        .setRowIndex(6) // 7-я строка
                        .setColumnIndex(20) // U столбец
                )
                .setRows(
                    (7..6 + studentCount).map { row ->
                        val range = "D$row:T$row"
                        RowData().setValues(
                            listOf(
                                CellData().setUserEnteredValue(
                                    ExtendedValue().setFormulaValue(
                                        "=ROUND(100*(COUNTIF($range,1)/17), 1) & \"%\""
                                    )
                                )
                            )
                        )
                    }
                )
                .setFields("userEnteredValue")
        )
    }

    private fun createFormattingRequests(): List<Request> {
        return listOf(
            // Форматирование заголовков
            Request().setRepeatCell(
                RepeatCellRequest()
                    .setRange(
                        GridRange()
                            .setSheetId(0)
                            .setStartRowIndex(5)
                            .setEndRowIndex(6)
                            .setStartColumnIndex(2)
                            .setEndColumnIndex(21)
                    )
                    .setCell(
                        CellData()
                            .setUserEnteredFormat(
                                CellFormat()
                                    .setTextFormat(
                                        TextFormat()
                                            .setBold(true)
                                    )
                                    .setHorizontalAlignment("CENTER")
                            )
                    )
                    .setFields("userEnteredFormat.textFormat.bold,userEnteredFormat.horizontalAlignment")
            ),
            // Форматирование процентов посещаемости
            Request().setRepeatCell(
                RepeatCellRequest()
                    .setRange(
                        GridRange()
                            .setSheetId(0)
                            .setStartRowIndex(6)
                            .setEndRowIndex(36)
                            .setStartColumnIndex(20)
                            .setEndColumnIndex(21)
                    )
                    .setCell(
                        CellData()
                            .setUserEnteredFormat(
                                CellFormat()
                                    .setHorizontalAlignment("CENTER")
                            )
                    )
                    .setFields("userEnteredFormat.horizontalAlignment")
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