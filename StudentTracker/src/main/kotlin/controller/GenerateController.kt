package org.example.controller

import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayInputStream

@RestController
@RequestMapping("/api")
class GenerateController {

    @PostMapping("/generateTables", consumes = ["multipart/form-data"])
    fun generateTables(
        @RequestParam("jsonFile") jsonFile: MultipartFile,
        @RequestParam("xlsxFile") xlsxFile: MultipartFile
    ): String {
        // Обработка JSON
        val jsonContent = String(jsonFile.bytes)

        // Обработка XLSX
        val workbook = XSSFWorkbook(ByteArrayInputStream(xlsxFile.bytes))
        val sheet = workbook.getSheetAt(0)

        val excelData = StringBuilder()
        for (row in sheet) {
            for (cell in row) {
                excelData.append(cell.toString()).append("\t")
            }
            excelData.append("\n")
        }

        return """
            Успешно обработано:
            JSON файл: ${jsonFile.originalFilename} (${jsonContent.length} символов)
            Excel файл: ${xlsxFile.originalFilename}
            Первые 5 строк данных:
            ${excelData.toString().lines().take(5).joinToString("\n")}
        """.trimIndent()
    }
}