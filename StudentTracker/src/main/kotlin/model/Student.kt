package org.example.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "students")
data class Student(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val surname: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = true)
    val patronymic: String? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @ManyToOne
    @JoinColumn(name = "group_stream_id", nullable = false)
    val groupStream: GroupStream
) {
    constructor() : this(null, "", "", "", "", GroupStream(null, "", ""))
}