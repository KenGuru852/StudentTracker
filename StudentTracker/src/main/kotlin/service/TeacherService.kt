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
        val jsonString = String(file.bytes)

        val teachers = try {
            val jsonList: List<Map<String, String>> = jacksonObjectMapper().readValue(jsonString)
            jsonList.map {
                Teacher(
                    fullName = it["full_name"] ?: throw IllegalArgumentException("Отсутствует full_name"),
                    email = it["email"]
                )
            }
        } catch (e: Exception) {
            throw TeacherProcessingException("Ошибка парсинга JSON преподавателей: ${e.message}", e)
        }

        val existingTeachers = teacherRepository.findAll().map { it.fullName }.toSet()
        val newTeachers = teachers.filter { !existingTeachers.contains(it.fullName) }

        if (newTeachers.isNotEmpty()) {
            teacherRepository.saveAll(newTeachers)
        }

        return newTeachers.size
    }
}

class TeacherProcessingException(message: String, cause: Throwable?) :
    RuntimeException(message, cause)