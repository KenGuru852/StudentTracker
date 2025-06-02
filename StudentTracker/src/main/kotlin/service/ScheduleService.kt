package org.example.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.example.model.Schedule
import org.example.repository.ScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import kotlin.jvm.optionals.toList

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository
) {

    private val objectMapper = jacksonObjectMapper()

    @Transactional
    fun processScheduleJsonFile(file: MultipartFile): List<Schedule> {
        if (scheduleRepository.findAll().isNotEmpty()){
            return scheduleRepository.findById(1).toList()
        }

        val jsonString = String(file.bytes)
        val schedules = parseSchedulesFromJson(jsonString)
        return scheduleRepository.saveAll(schedules)

    }

    private fun parseSchedulesFromJson(jsonString: String): List<Schedule> {

        return try {
            val jsonList: List<Map<String, Any>> = objectMapper.readValue(jsonString)
            jsonList.map { Schedule.fromJson(it) }
        } catch (e: Exception) {
            throw ScheduleProcessingException("Ошибка парсинга JSON: ${e.message}", e)
        }

    }
}

class ScheduleProcessingException(message: String, cause: Throwable?) :
    RuntimeException(message, cause)