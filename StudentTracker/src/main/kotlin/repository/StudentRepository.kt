package org.example.repository

import org.example.model.Student
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface StudentRepository : JpaRepository<Student, Long> {
    fun findByGroupStreamGroupName(groupName: String): List<Student>

    @Query("SELECT s FROM Student s")
    override fun findAll(): List<Student>
}