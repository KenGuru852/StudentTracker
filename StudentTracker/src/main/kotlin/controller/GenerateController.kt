package org.example.controller

import org.example.service.ScheduleService
import org.example.service.StudentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api")
class GenerateController(
    private val studentService: StudentService,
    private val scheduleService: ScheduleService
) {

    @PostMapping("/generateTables", consumes = ["multipart/form-data"])
    fun generateTables(
        @RequestParam("jsonFile") scheduleJsonFile: MultipartFile,
        @RequestParam("xlsxFile") studentsExcelFile: MultipartFile
    ): ResponseEntity<String> {

        return try {
            val students = studentService.processStudentsExcelFile(studentsExcelFile)
            val schedules = scheduleService.processScheduleJsonFile(scheduleJsonFile)

            val result = """
                Успешно обработано:
                JSON: ${scheduleJsonFile.originalFilename} (${schedules.size} записей)
                Excel: ${studentsExcelFile.originalFilename} (${students.size} студентов)
            """.trimIndent()

            ResponseEntity.ok(result)

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Ошибка обработки файлов: ${e.message}")
        }

    }
}