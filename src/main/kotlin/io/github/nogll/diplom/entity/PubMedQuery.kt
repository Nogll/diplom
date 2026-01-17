package io.github.nogll.diplom.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "pubmed_queries")
class PubMedQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    lateinit var chat: Chat
    
    @Column(nullable = false, columnDefinition = "TEXT")
    var query: String = ""
}

