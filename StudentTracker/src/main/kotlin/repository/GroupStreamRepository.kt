package org.example.repository

import org.example.model.GroupStream
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface GroupStreamRepository : JpaRepository<GroupStream, Long> {
    fun findByGroupName(groupName: String): GroupStream?

    @Query("SELECT gs FROM GroupStream gs")
    override fun findAll(): List<GroupStream>
}