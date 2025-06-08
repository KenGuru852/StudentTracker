package org.example.model

import jakarta.persistence.*
import org.example.repository.TeacherRepository

@Entity
@Table(name = "teachers")
data class Teacher(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val fullName: String,

    @Column(nullable = false, unique = true)
    val email: String
) {
    constructor() : this(null, "", "")
}