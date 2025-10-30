package io.github.nogll.diplom.entity

import jakarta.persistence.*

@Entity
@Table(name = "source")
class Source {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    lateinit var article: Article
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    lateinit var model: Model
    
    @Column(columnDefinition = "TEXT")
    var rawResponse: String? = null
}

