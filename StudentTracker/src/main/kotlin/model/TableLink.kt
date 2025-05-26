package org.example.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "table_links", uniqueConstraints = [
    UniqueConstraint(columnNames = ["stream_name", "subject"])
])
data class TableLink(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "stream_name", nullable = false)
    val streamName: String,

    @Column(nullable = false)
    val subject: String,

    @Column(nullable = false)
    val link: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    constructor() : this(null, "", "", "", LocalDateTime.now())
}