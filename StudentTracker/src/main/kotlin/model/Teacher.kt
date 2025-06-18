package org.example.model

import jakarta.persistence.*

@Entity
@Table(name = "teachers")
data class Teacher(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "full_name", nullable = false)
    val fullName: String,

    @Column
    val email: String? = null
) {
    constructor() : this(null, "", null)
}