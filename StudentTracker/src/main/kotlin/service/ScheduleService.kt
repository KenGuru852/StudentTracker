package org.example.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.example.model.Schedule
import org.example.repository.ScheduleRepository
import org.example.repository.TeacherRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import kotlin.jvm.optionals.toList

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val teacherRepository: TeacherRepository
) {
    @Transactional
    fun processScheduleJsonFile(file: MultipartFile): List<Schedule> {
        if (scheduleRepository.findAll().isNotEmpty()) {
            return scheduleRepository.findById(1).toList()
        }

        return try {
            val jsonList: List<Map<String, Any>> = jacksonObjectMapper().readValue(String(file.bytes))
            scheduleRepository.saveAll(jsonList.map { Schedule.fromJson(it, teacherRepository) })
        } catch (e: Exception) {
            throw ScheduleProcessingException("Ошибка обработки файла расписания: ${e.message}", e)
        }
    }
}

class ScheduleProcessingException(message: String, cause: Throwable?) :
    RuntimeException(message, cause)