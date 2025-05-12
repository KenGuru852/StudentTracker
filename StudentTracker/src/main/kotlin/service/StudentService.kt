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
    fun processExcelFile(file: MultipartFile): List<Student> {

        val students = mutableListOf<Student>()

        val groupStreamCache = mutableMapOf<String, GroupStream>()

        try {
            val workbook = XSSFWorkbook(ByteArrayInputStream(file.bytes))

            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)

                for (rowIndex in 0..sheet.lastRowNum) {
                    val row: Row = sheet.getRow(rowIndex) ?: continue

                    val surname = row.getCell(0)?.toString()?.trim() ?: continue
                    val name = row.getCell(1)?.toString()?.trim() ?: continue
                    val patronymic = row.getCell(2)?.toString()?.trim()
                    val stream = row.getCell(3)?.toString()?.trim() ?: continue
                    val group = row.getCell(4)?.toString()?.trim() ?: continue
                    val email = row.getCell(5)?.toString()?.trim() ?: continue

                    val groupStreamKey = "$group-$stream"

                    val groupStream = groupStreamCache.getOrPut(groupStreamKey) {
                        groupStreamRepository.findByGroupName(group) ?:
                        groupStreamRepository.save(GroupStream(groupName = group, streamName = stream))
                    }

                    val student = Student(
                        surname = surname,
                        name = name,
                        patronymic = patronymic,
                        email = email,
                        groupStream = groupStream
                    )
                    students.add(student)
                }
            }

            studentRepository.saveAll(students)

        } catch (e: Exception) {
            throw ExcelProcessingException("Ошибка обработки Excel файла", e)
        }

        return students
    }
}

class ExcelProcessingException(message: String, cause: Throwable?) :
    RuntimeException(message, cause)