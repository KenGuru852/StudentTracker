package org.example.service

import org.example.repository.GroupStreamRepository
import org.example.repository.ScheduleRepository
import org.example.repository.StudentRepository
import org.example.repository.TableLinkRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DatabaseService(
    private val scheduleRepository: ScheduleRepository,
    private val studentRepository: StudentRepository,
    private val groupStreamRepository: GroupStreamRepository,
    private val tableLinkRepository: TableLinkRepository,
    private val logger: Logger? = LoggerFactory.getLogger(TableService::class.java)
) {
    @Transactional
    fun clearAllData() {
        return try {
            logger!!.info("Starting data cleanup process")

            // 1. Сначала удаляем табличные ссылки (нет зависимостей)
            tableLinkRepository.deleteAllInBatch()
            logger.info("Cleared table_links")

            // 2. Затем студентов (зависит от group_streams)
            studentRepository.deleteAllInBatch()
            logger.info("Cleared students")

            // 3. Затем расписание (зависит от group_streams через group_name)
            scheduleRepository.deleteAllInBatch()
            logger.info("Cleared schedule")

            // 4. В конце группы (нет зависимостей, но на них ссылаются другие таблицы)
            groupStreamRepository.deleteAllInBatch()
            logger.info("Cleared group_streams")
        } catch (e: Exception) {
            throw DatabaseProcessingException("Ошибка удаления данных: ${e.message}", e)
        }
    }

    class DatabaseProcessingException(message: String, cause: Throwable?) :
        RuntimeException(message, cause)
}