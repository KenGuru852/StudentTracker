package org.example.service

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.example.model.GroupStream
import org.example.model.Student
import org.example.repository.GroupStreamRepository
import org.example.repository.StudentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream

@Service
class StudentService(
    private val studentRepository: StudentRepository,
    private val groupStreamRepository: GroupStreamRepository
) {

    @Transactional
    fun processStudentsExcelFile(file: MultipartFile): List<Student> {
        val existingStudents = studentRepository.findAll().associateBy {
            "${it.surname}_${it.name}_${it.patronymic}_${it.email}_${it.groupStream.groupName}"
        }

        val studentsToSave = mutableListOf<Student>()
        val groupStreamCache = mutableMapOf<String, GroupStream>()

        try {
            val workbook = XSSFWorkbook(ByteArrayInputStream(file.bytes))
            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                for (rowIndex in 1..sheet.lastRowNum) {
                    val row: Row = sheet.getRow(rowIndex) ?: continue
                    try {
                        val (group, stream) = getGroupAndStream(row)
                        val groupStreamKey = "$group-$stream"

                        val groupStream = groupStreamCache.getOrPut(groupStreamKey) {
                            groupStreamRepository.findByGroupName(group) ?:
                            groupStreamRepository.save(GroupStream(groupName = group, streamName = stream))
                        }

                        val studentKey = getStudentKey(row, groupStream)
                        if (!existingStudents.containsKey(studentKey)) {
                            val student = createStudent(row, groupStream)
                            val savedStudent = studentRepository.save(student)

                            if (isHeadman(row)) {
                                groupStream.headman = savedStudent
                                groupStreamRepository.save(groupStream)
                            }

                            studentsToSave.add(savedStudent)
                        }
                    } catch (e: IllegalArgumentException) {
                        throw ExcelProcessingException("Ошибка в строке ${rowIndex + 1}: ${e.message}", e)
                    }
                }
            }
            return studentsToSave
        } catch (e: Exception) {
            throw ExcelProcessingException("Ошибка обработки Excel файла: ${e.message}", e)
        }
    }

    private fun getGroupAndStream(row: Row): Pair<String, String> {
        val stream = row.getCell(4)?.toString()?.trim()
            ?: throw IllegalArgumentException("Название потока не может быть пустым")
        val group = row.getCell(5)?.toString()?.trim()
            ?: throw IllegalArgumentException("Название группы не может быть пустым")
        return Pair(group, stream)
    }

    private fun getStudentKey(row: Row, groupStream: GroupStream): String {
        val surname = row.getCell(1)?.toString()?.trim() ?: ""
        val name = row.getCell(2)?.toString()?.trim() ?: ""
        val patronymic = row.getCell(3)?.toString()?.trim() ?: ""
        val email = row.getCell(6)?.toString()?.trim() ?: ""
        return "${surname}_${name}_${patronymic}_${email}_${groupStream.groupName}"
    }

    private fun createStudent(row: Row, groupStream: GroupStream): Student {
        return Student(
            surname = row.getCell(1)?.toString()?.trim()
                ?: throw IllegalArgumentException("Фамилия не может быть пустой"),
            name = row.getCell(2)?.toString()?.trim()
                ?: throw IllegalArgumentException("Имя не может быть пустым"),
            patronymic = row.getCell(3)?.toString()?.trim(),
            email = row.getCell(6)?.toString()?.trim(),
            groupStream = groupStream
        )
    }

    private fun isHeadman(row: Row): Boolean {
        return row.getCell(7)
            ?.toString()
            ?.trim()
            ?.let { it == "+" }
            ?: false
    }
}

class ExcelProcessingException(message: String, cause: Throwable?) :
    RuntimeException(message, cause)