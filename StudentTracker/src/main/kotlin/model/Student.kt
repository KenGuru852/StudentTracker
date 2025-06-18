package org.example.model

import jakarta.persistence.*
import org.apache.poi.ss.usermodel.Row
import org.example.repository.GroupStreamRepository
import org.example.repository.StudentRepository

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

    companion object {
        fun fromExcelRow(
            row: Row,
            groupStreamRepository: GroupStreamRepository,
            studentRepository: StudentRepository, // Добавляем репозиторий студентов
            groupStreamCache: MutableMap<String, GroupStream> = mutableMapOf()
        ): Student {
            val surname = row.getCell(1)?.toString()?.trim()
                ?: throw IllegalArgumentException("Фамилия не может быть пустой")
            val name = row.getCell(2)?.toString()?.trim()
                ?: throw IllegalArgumentException("Имя не может быть пустым")
            val patronymic = row.getCell(3)?.toString()?.trim()
            val stream = row.getCell(4)?.toString()?.trim()
                ?: throw IllegalArgumentException("Название потока не может быть пустым")
            val group = row.getCell(5)?.toString()?.trim()
                ?: throw IllegalArgumentException("Название группы не может быть пустым")
            val email = row.getCell(6)?.toString()?.trim()

            val isHeadman = row.getCell(7)
                ?.toString()
                ?.trim()
                ?.let { it == "+" }
                ?: false

            val groupStreamKey = "$group-$stream"
            val groupStream = groupStreamCache.getOrPut(groupStreamKey) {
                groupStreamRepository.findByGroupName(group) ?: groupStreamRepository.save(
                    GroupStream(
                        groupName = group,
                        streamName = stream
                    )
                )
            }

            val student = studentRepository.save(
                Student(
                    surname = surname,
                    name = name,
                    patronymic = patronymic,
                    email = email,
                    groupStream = groupStream
                )
            )

            if (isHeadman) {
                groupStream.headman = student
                groupStreamRepository.save(groupStream)
            }

            return student
        }
    }
}