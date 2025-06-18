package org.example.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.example.model.Schedule
import org.example.model.Teacher
import org.example.repository.ScheduleRepository
import org.example.repository.TeacherRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val teacherRepository: TeacherRepository
) {
    @Transactional
    fun processScheduleJsonFile(file: MultipartFile): List<Schedule> {
        val existingSchedules = scheduleRepository.findAll().associateBy {
            "${it.dayOfWeek}_${it.startTime}_${it.subject}_${it.teacher.fullName}_${it.groupName}"
        }

        return try {
            val jsonList: List<Map<String, Any>> = jacksonObjectMapper().readValue(String(file.bytes))
            val newSchedules = jsonList.mapNotNull { json ->
                val tempSchedule = createTempSchedule(json, teacherRepository)
                val scheduleKey =
                    "${tempSchedule.dayOfWeek}_${tempSchedule.startTime}_${tempSchedule.subject}_${tempSchedule.teacher.fullName}_${tempSchedule.groupName}"

                if (!existingSchedules.containsKey(scheduleKey)) {
                    scheduleRepository.save(tempSchedule)
                } else {
                    null
                }
            }
            newSchedules
        } catch (e: Exception) {
            throw ScheduleProcessingException("Ошибка обработки файла расписания: ${e.message}", e)
        }
    }

    private fun createTempSchedule(json: Map<String, Any>, teacherRepository: TeacherRepository): Schedule {
        val timeString = json["ВремяНачала"] as String
        val dayString = (json["ДеньНедели"] as String).lowercase()
        val teacherName = json["ФизическоеЛицо"] as String

        val teacher = teacherRepository.findByFullName(teacherName)
            ?: throw IllegalArgumentException("Преподавателя $teacherName не существует")

        return Schedule(
            startTime = LocalTime.parse(timeString, timeFormatter),
            dayOfWeek = dayOfWeekMap[dayString]
                ?: throw IllegalArgumentException("Неизвестный день недели: $dayString"),
            groupName = json["Группа"] as String,
            teacher = teacher,
            subject = json["Дисциплина"] as String
        )
    }

    companion object {
        private val timeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm:ss")
        private val dayOfWeekMap = mapOf(
            "понедельник" to DayOfWeek.MONDAY,
            "вторник" to DayOfWeek.TUESDAY,
            "среда" to DayOfWeek.WEDNESDAY,
            "четверг" to DayOfWeek.THURSDAY,
            "пятница" to DayOfWeek.FRIDAY,
            "суббота" to DayOfWeek.SATURDAY,
            "воскресенье" to DayOfWeek.SUNDAY
        )
    }
}

class ScheduleProcessingException(message: String, cause: Throwable?) :
    RuntimeException(message, cause)