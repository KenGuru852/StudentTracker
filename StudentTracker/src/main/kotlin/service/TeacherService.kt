package org.example.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.example.model.Teacher
import org.example.repository.TeacherRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class TeacherService(
    private val teacherRepository: TeacherRepository
) {
    @Transactional
    fun processTeachersJsonFile(file: MultipartFile): Int {
        val existingTeachers = teacherRepository.findAll().associateBy { it.fullName }

        val teachers = try {
            val jsonList: List<Map<String, String>> = jacksonObjectMapper().readValue(String(file.bytes))
            jsonList.mapNotNull {
                val fullName = it["full_name"] ?: throw IllegalArgumentException("Отсутствует ФИО преподавателя")
                if (!existingTeachers.containsKey(fullName)) {
                    Teacher(
                        fullName = fullName,
                        email = it["email"]
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            throw TeacherProcessingException("Ошибка парсинга JSON преподавателей: ${e.message}", e)
        }

        return if (teachers.isNotEmpty()) {
            teacherRepository.saveAll(teachers).size
        } else {
            0
        }
    }
}

class TeacherProcessingException(message: String, cause: Throwable?) :
    RuntimeException(message, cause)