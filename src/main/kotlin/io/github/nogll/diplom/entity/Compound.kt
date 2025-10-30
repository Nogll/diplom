package io.github.nogll.diplom.entity

import jakarta.persistence.*

@Entity
@Table(name = "compound")
data class Compound(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column
    val name: String? = null
)

