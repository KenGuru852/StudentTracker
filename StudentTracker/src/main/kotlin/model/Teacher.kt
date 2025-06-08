package org.example.model

import jakarta.persistence.*
import org.example.repository.TeacherRepository
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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

    companion object {
        fun fromJson(json: Map<String, Any>, teacherRepository: TeacherRepository): Teacher {
            val fullName = json["full_name"] as String
            val email = json["email"] as String

            val teacher = teacherRepository.findByFullName(fullName) ?:
            teacherRepository.save(Teacher(fullName = fullName, email = email))

            return Teacher(
                fullName = fullName,
                email = email
            )
        }
    }
}