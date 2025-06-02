package org.example.model

import jakarta.persistence.*

@Entity
@Table(name = "group_streams")
data class GroupStream(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "group_name", nullable = false, unique = true)
    val groupName: String,

    @Column(name = "stream_name", nullable = false)
    val streamName: String
) {
    constructor() : this(null, "", "")
}