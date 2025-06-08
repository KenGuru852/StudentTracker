package org.example.repository

import org.example.model.Schedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ScheduleRepository : JpaRepository<Schedule, Long> {
    fun findByGroupNameIn(groupNames: List<String>): List<Schedule>

    @Query("SELECT DISTINCT s.subject FROM Schedule s WHERE s.groupName IN :groupNames")
    fun findDistinctSubjectsByGroups(groupNames: List<String>): List<String>

    @Query("SELECT s FROM Schedule s")
    override fun findAll(): List<Schedule>

    fun findByGroupName(groupName: String): List<Schedule>
}