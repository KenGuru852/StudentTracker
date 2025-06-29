package org.example.controller

import org.example.repository.GroupStreamRepository
import org.example.repository.ScheduleRepository
import org.example.repository.StudentRepository
import org.example.repository.TableLinkRepository
import org.example.service.DatabaseService
import org.example.service.ScheduleService
import org.example.service.StudentService
import org.example.service.TableService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api")
class GenerateController(
    private val studentService: StudentService,
    private val scheduleService: ScheduleService,
    private val tableService: TableService,
    private val databaseService: DatabaseService,
    private val logger: Logger? = LoggerFactory.getLogger(TableService::class.java)
) {

    @PostMapping("/generateTables", consumes = ["multipart/form-data"])
    fun generateTables(
        @RequestParam("jsonFile") scheduleJsonFile: MultipartFile,
        @RequestParam("xlsxFile") studentsExcelFile: MultipartFile
    ): ResponseEntity<Map<String, List<String>>> {
        return try {
            val students = studentService.processStudentsExcelFile(studentsExcelFile)
            val schedules = scheduleService.processScheduleJsonFile(scheduleJsonFile)

            val allTables = tableService.createAttendanceSheetsForAllStreams()

            // Преобразуем в Map<String, List<String>> для JSON
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
        @RequestParam(required = false) subject: String?
    ): ResponseEntity<List<Map<String, String>>> {
        return try {
            val links = tableService.getFilteredTableLinks(stream, subject)
                .map {
                    mapOf(
                        "stream" to it.streamName,
                        "subject" to it.subject,
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
                .body("Ошибка при очистке данных: ${e.message ?: "Unknown error"}")
        }
    }
}