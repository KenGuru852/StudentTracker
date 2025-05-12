package org.example.controller

import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.example.service.ExcelProcessingException
import org.example.service.StudentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.io.ByteArrayInputStream

@RestController
@RequestMapping("/api")
class GenerateController(
    private val studentService: StudentService
) {
    @PostMapping("/generateTables", consumes = ["multipart/form-data"])
    fun generateTables(
        @RequestParam("jsonFile") scheduleJsonFile: MultipartFile,
        @RequestParam("xlsxFile") studentsExcelFile: MultipartFile
    ): ResponseEntity<String> {

        return try {
            val jsonContent = String(scheduleJsonFile.bytes)

            val excelData = studentService.processExcelFile(studentsExcelFile)

            val result = """
            Успешно обработано:
            JSON: ${scheduleJsonFile.originalFilename}
            Excel: ${studentsExcelFile.originalFilename}
            Количество добавленных студентов: ${excelData.size}
        """

            ResponseEntity.ok(result)

        } catch (e: ExcelProcessingException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Ошибка обработки Excel: ${e.message}")
        }
    }

}