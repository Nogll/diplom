package io.github.nogll.diplom.entity

import jakarta.persistence.*

@Entity
@Table(name = "plant")
data class Plant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val name: String
)

