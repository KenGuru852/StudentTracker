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
    private val objectMapper = jacksonObjectMapper()

    @Transactional
    fun processTeachersJsonFile(file: MultipartFile): Int {
        val jsonString = String(file.bytes)
        val teachers = parseTeachersFromJson(jsonString)

        val existingEmails = teacherRepository.findAll().map { it.email }.toSet()
        val newTeachers = teachers.filter { !existingEmails.contains(it.email) }

        if (newTeachers.isNotEmpty()) {
            teacherRepository.saveAll(newTeachers)
        }

        return newTeachers.size
    }

    private fun parseTeachersFromJson(jsonString: String): List<Teacher> {
        return try {
            val jsonList: List<Map<String, String>> = objectMapper.readValue(jsonString)
            jsonList.map {
                Teacher(
                    fullName = it["full_name"] ?: throw IllegalArgumentException("Отсутствует full_name"),
                    email = it["email"] ?: throw IllegalArgumentException("Отсутствует email")
                )
            }
        } catch (e: Exception) {
            throw TeacherProcessingException("Ошибка парсинга JSON преподавателей: ${e.message}", e)
        }
    }
}

class TeacherProcessingException(message: String, cause: Throwable?) :
    RuntimeException(message, cause)