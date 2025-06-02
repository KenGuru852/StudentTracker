package org.example.model

import jakarta.persistence.*
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

    @Column(nullable = false)
    val teacher: String,

    @Column(nullable = false)
    val subject: String
) {
    constructor() : this(null, LocalTime.now(), DayOfWeek.FRIDAY, "", "", "")
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

        fun fromJson(json: Map<String, Any>): Schedule {
            val timeString = json["ВремяНачала"] as String
            val dayString = (json["ДеньНедели"] as String).lowercase()

            return Schedule(
                startTime = LocalTime.parse(timeString, timeFormatter),
                dayOfWeek = dayOfWeekMap[dayString]
                    ?: throw IllegalArgumentException("Неизвестный день недели: $dayString"),
                groupName = json["Группа"] as String,
                teacher = json["ФизическоеЛицо"] as String,
                subject = json["Дисциплина"] as String
            )
        }
    }
}