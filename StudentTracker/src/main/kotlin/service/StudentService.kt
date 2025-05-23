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
        val students = mutableListOf<Student>()
        val groupStreamCache = mutableMapOf<String, GroupStream>()

        try {

            val workbook = XSSFWorkbook(ByteArrayInputStream(file.bytes))

            processExcelSheets(workbook, groupStreamCache, students)

            studentRepository.saveAll(students)

        } catch (e: Exception) {
            throw ExcelProcessingException("Ошибка обработки Excel файла", e)
        }

        return students
    }

    private fun processExcelSheets(
        workbook: XSSFWorkbook,
        groupStreamCache: MutableMap<String, GroupStream>,
        students: MutableList<Student>
    ) {
        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)

            for (rowIndex in 1..sheet.lastRowNum) {
                val row: Row = sheet.getRow(rowIndex) ?: continue
                processStudentRow(row, groupStreamCache, students)
            }
        }
    }

    private fun processStudentRow(
        row: Row,
        groupStreamCache: MutableMap<String, GroupStream>,
        students: MutableList<Student>
    ) {
        val surname = row.getCell(1)?.toString()?.trim() ?: return
        val name = row.getCell(2)?.toString()?.trim() ?: return
        val patronymic = row.getCell(3)?.toString()?.trim()
        val stream = row.getCell(4)?.toString()?.trim() ?: return
        val group = row.getCell(5)?.toString()?.trim() ?: return
        val email = row.getCell(6)?.toString()?.trim() ?: return

        val groupStreamKey = "$group-$stream"

        val groupStream = groupStreamCache.getOrPut(groupStreamKey) {
            groupStreamRepository.findByGroupName(group) ?: groupStreamRepository.save(
                GroupStream(
                    groupName = group,
                    streamName = stream
                )
            )
        }

        students.add(
            Student(
                surname = surname,
                name = name,
                patronymic = patronymic,
                email = email,
                groupStream = groupStream
            )
        )
    }
}

class ExcelProcessingException(message: String, cause: Throwable?) :
    RuntimeException(message, cause)