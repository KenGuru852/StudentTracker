package org.example.service

import org.example.model.TableLink
import org.example.repository.*
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
    private val teacherRepository: TeacherRepository,
    private val logger: Logger? = LoggerFactory.getLogger(TableService::class.java)
) {
    @Transactional
    fun clearAllData() {
        return try {
            logger!!.info("Starting data cleanup process")

            tableLinkRepository.deleteAllInBatch()
            logger.info("Cleared table_links")

            studentRepository.deleteAllInBatch()
            logger.info("Cleared students")

            scheduleRepository.deleteAllInBatch()
            logger.info("Cleared schedule")

            teacherRepository.deleteAllInBatch()
            logger.info("Cleared teachers")

            groupStreamRepository.deleteAllInBatch()
            logger.info("Cleared group_streams")
        } catch (e: Exception) {
            throw DatabaseProcessingException("Ошибка удаления данных: ${e.message}", e)
        }
    }

    fun getFilteredTableLinks(streamName: String?, subject: String?, teacherName: String?): List<TableLink> {
        logger!!.info("Filtering table links. Stream: '$streamName', Subject: '$subject', Teacher: '$teacherName'")

        return try {
            val result = when {
                streamName != null && subject != null && teacherName != null -> {
                    logger.debug("All filters present")
                    tableLinkRepository.findByStreamNameContainingIgnoreCaseAndSubjectContainingIgnoreCaseAndTeacherNameContainingIgnoreCase(
                        streamName, subject, teacherName
                    )
                }

                streamName != null && subject != null -> {
                    logger.debug("Stream and subject filters present")
                    tableLinkRepository.findByStreamNameContainingIgnoreCaseAndSubjectContainingIgnoreCase(
                        streamName, subject
                    )
                }

                streamName != null && teacherName != null -> {
                    logger.debug("Stream and teacher filters present")
                    tableLinkRepository.findByStreamNameContainingIgnoreCaseAndTeacherNameContainingIgnoreCase(
                        streamName, teacherName
                    )
                }

                subject != null && teacherName != null -> {
                    logger.debug("Subject and teacher filters present")
                    tableLinkRepository.findBySubjectContainingIgnoreCaseAndTeacherNameContainingIgnoreCase(
                        subject, teacherName
                    )
                }

                streamName != null -> {
                    logger.debug("Only stream filter present")
                    tableLinkRepository.findByStreamNameContainingIgnoreCase(streamName)
                }

                subject != null -> {
                    logger.debug("Only subject filter present")
                    tableLinkRepository.findBySubjectContainingIgnoreCase(subject)
                }

                teacherName != null -> {
                    logger.debug("Only teacher filter present")
                    tableLinkRepository.findByTeacherNameContainingIgnoreCase(teacherName)
                }

                else -> {
                    logger.debug("No filters, returning all")
                    tableLinkRepository.findAll()
                }
            }

            logger.info("Found ${result.size} matching table links")
            result
        } catch (e: Exception) {
            logger.error("Error in getFilteredTableLinks", e)
            throw e
        }
    }

    class DatabaseProcessingException(message: String, cause: Throwable?) :
        RuntimeException(message, cause)
}