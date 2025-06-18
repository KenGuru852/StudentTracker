package org.example.model

import jakarta.persistence.*

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

    @Column(nullable = true)
    val email: String? = null,

    @ManyToOne
    @JoinColumn(name = "group_stream_id", nullable = false)
    val groupStream: GroupStream,

    @OneToOne(mappedBy = "headman")
    val headedGroup: GroupStream? = null
) {
    constructor() : this(null, "", "", null, null, GroupStream(), null)
}