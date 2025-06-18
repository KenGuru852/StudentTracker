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
import kotlin.jvm.optionals.toList

@Service
class StudentService(
    private val studentRepository: StudentRepository,
    private val groupStreamRepository: GroupStreamRepository
) {

    @Transactional
    fun processStudentsExcelFile(file: MultipartFile): List<Student> {
        if (studentRepository.findAll().isNotEmpty()) {
            return studentRepository.findById(1).toList()
        }

        val students = mutableListOf<Student>()
        val groupStreamCache = mutableMapOf<String, GroupStream>()

        try {
            val workbook = XSSFWorkbook(ByteArrayInputStream(file.bytes))
            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                for (rowIndex in 1..sheet.lastRowNum) {
                    val row: Row = sheet.getRow(rowIndex) ?: continue
                    try {
                        students.add(Student.fromExcelRow(row, groupStreamRepository, studentRepository, groupStreamCache))
                    } catch (e: IllegalArgumentException) {
                        throw ExcelProcessingException("Ошибка в строке ${rowIndex + 1}: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            throw ExcelProcessingException("Ошибка обработки Excel файла: ${e.message}", e)
        }

        return students
    }
}

class ExcelProcessingException(message: String, cause: Throwable?) :
    RuntimeException(message, cause)