package org.example.controller

import org.example.service.DatabaseService
import org.example.service.ScheduleService
import org.example.service.StudentService
import org.example.service.TableService
import org.example.service.TeacherService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api")
class GenerateController(
    private val studentService: StudentService,
    private val scheduleService: ScheduleService,
    private val tableService: TableService,
    private val databaseService: DatabaseService,
    private val teacherService: TeacherService,
    private val logger: Logger? = LoggerFactory.getLogger(TableService::class.java)
) {

    @PostMapping("/generateTables", consumes = ["multipart/form-data"])
    fun generateTables(
        @RequestParam("scheduleFile") scheduleJsonFile: MultipartFile,
        @RequestParam("studentFile") studentsExcelFile: MultipartFile,
        @RequestParam("teachersFile") teachersFile: MultipartFile
    ): ResponseEntity<Map<String, List<String>>> {
        return try {
            teacherService.processTeachersJsonFile(teachersFile)
            studentService.processStudentsExcelFile(studentsExcelFile)
            scheduleService.processScheduleJsonFile(scheduleJsonFile)

            val allTables = tableService.createAttendanceSheetsForAllStreams()

            val result = allTables.mapValues { entry ->
                entry.value.takeIf { it.isNotEmpty() } ?: listOf("No link generated")
            }

            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to listOf("Ошибка обработки файлов: ${e.message}")))
        }
    }

    @GetMapping("/getFilteredLinks", produces = ["application/json"])
    fun getFilteredLinks(
        @RequestParam(required = false) stream: String?,
        @RequestParam(required = false) subject: String?,
        @RequestParam(required = false) teacher: String?
    ): ResponseEntity<List<Map<String, String>>> {
        return try {
            val links = databaseService.getFilteredTableLinks(stream, subject, teacher)
                .map {
                    mapOf(
                        "stream" to it.streamName,
                        "subject" to it.subject,
                        "teacher" to it.teacherName,
                        "link" to it.link
                    )
                }
            ResponseEntity.ok(links)
        } catch (e: Exception) {
            logger!!.error("Error while filtering tables", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(listOf(mapOf("error" to "Ошибка при фильтрации таблиц: ${e.message}")))
        }
    }

    @PostMapping("/clearAllData")
    fun clearAllData(): ResponseEntity<String> {
        return try {
            databaseService.clearAllData()

            ResponseEntity.ok("Все данные успешно очищены")
        } catch (e: Exception) {
            logger!!.error("Error clearing data", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Ошибка при очистке данных: ${e.message ?: "Неизвестная ошибка"}")
        }
    }
}