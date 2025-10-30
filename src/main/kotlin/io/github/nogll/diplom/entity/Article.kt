package io.github.nogll.diplom.entity

import jakarta.persistence.*

@Entity
@Table(name = "article")
data class Article(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    val url: String,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    val title: String,
    
    @Column(columnDefinition = "TEXT")
    val abstract: String? = null
)

