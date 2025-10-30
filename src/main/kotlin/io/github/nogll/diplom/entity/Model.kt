package io.github.nogll.diplom.entity

import jakarta.persistence.*

@Entity
@Table(name = "model")
data class Model(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null
)

