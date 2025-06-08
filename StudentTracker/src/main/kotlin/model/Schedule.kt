package org.example.model

import jakarta.persistence.*
import org.example.repository.TeacherRepository
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Entity
@Table(name = "schedule")
data class Schedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: DayOfWeek,

    @Column(name = "group_name", nullable = false)
    val groupName: String,

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    val teacher: Teacher,

    @Column(nullable = false)
    val subject: String
) {
    constructor() : this(null, LocalTime.now(), DayOfWeek.FRIDAY, "", Teacher(), "")

    companion object {
        private val timeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm:ss")
        private val dayOfWeekMap = mapOf(
            "понедельник" to DayOfWeek.MONDAY,
            "вторник" to DayOfWeek.TUESDAY,
            "среда" to DayOfWeek.WEDNESDAY,
            "четверг" to DayOfWeek.THURSDAY,
            "пятница" to DayOfWeek.FRIDAY,
            "суббота" to DayOfWeek.SATURDAY,
            "воскресенье" to DayOfWeek.SUNDAY
        )

        fun fromJson(json: Map<String, Any>, teacherRepository: TeacherRepository): Schedule {
            val timeString = json["ВремяНачала"] as String
            val dayString = (json["ДеньНедели"] as String).lowercase()
            val teacherName = json["ФизическоеЛицо"] as String
            val teacherEmail = "student_tracker_teacher_test@mail.ru" // Default email

            val teacher = teacherRepository.findByFullName(teacherName) ?:
            teacherRepository.save(Teacher(fullName = teacherName, email = teacherEmail))

            return Schedule(
                startTime = LocalTime.parse(timeString, timeFormatter),
                dayOfWeek = dayOfWeekMap[dayString]
                    ?: throw IllegalArgumentException("Неизвестный день недели: $dayString"),
                groupName = json["Группа"] as String,
                teacher = teacher,
                subject = json["Дисциплина"] as String
            )
        }
    }
}