package org.example.model

import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalTime

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
}