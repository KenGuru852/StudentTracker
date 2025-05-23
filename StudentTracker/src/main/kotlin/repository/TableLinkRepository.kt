package org.example.repository

import org.example.model.TableLink
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TableLinkRepository : JpaRepository<TableLink, Long> {
    fun findByStreamNameAndSubject(streamName: String, subject: String): TableLink?
}