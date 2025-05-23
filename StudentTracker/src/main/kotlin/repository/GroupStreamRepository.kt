package org.example.repository

import org.example.model.GroupStream
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface GroupStreamRepository : JpaRepository<GroupStream, Long> {
    fun findByGroupName(groupName: String): GroupStream?
    fun findByStreamName(streamName: String): List<GroupStream>

    @Query("SELECT DISTINCT gs.streamName FROM GroupStream gs")
    fun findAllStreamNames(): List<String>

    @Query("SELECT gs.groupName FROM GroupStream gs WHERE gs.streamName = :streamName")
    fun findGroupNamesByStreamName(streamName: String): List<String>
}