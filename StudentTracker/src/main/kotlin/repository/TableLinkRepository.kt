package org.example.repository

import org.example.model.TableLink
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TableLinkRepository : JpaRepository<TableLink, Long> {
    fun findByStreamNameContainingIgnoreCase(streamName: String): List<TableLink>
    fun findBySubjectContainingIgnoreCase(subject: String): List<TableLink>
    fun findByTeacherNameContainingIgnoreCase(teacherName: String): List<TableLink>
    fun findByStreamNameContainingIgnoreCaseAndSubjectContainingIgnoreCase(
        streamName: String,
        subject: String
    ): List<TableLink>
    fun findByStreamNameContainingIgnoreCaseAndTeacherNameContainingIgnoreCase(
        streamName: String,
        teacherName: String
    ): List<TableLink>
    fun findBySubjectContainingIgnoreCaseAndTeacherNameContainingIgnoreCase(
        subject: String,
        teacherName: String
    ): List<TableLink>
    fun findByStreamNameContainingIgnoreCaseAndSubjectContainingIgnoreCaseAndTeacherNameContainingIgnoreCase(
        streamName: String,
        subject: String,
        teacherName: String
    ): List<TableLink>
}