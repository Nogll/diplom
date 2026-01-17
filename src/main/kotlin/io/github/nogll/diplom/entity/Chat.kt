package io.github.nogll.diplom.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "chats")
class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ChatStatus = ChatStatus.NEW
    
    @Column(columnDefinition = "TEXT")
    var userMessage: String? = null
    
    @Column(columnDefinition = "TEXT")
    var keywords: String? = null // JSON array as string
    
    @Column(columnDefinition = "TEXT")
    var summary: String? = null
    
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
    
    @Column(nullable = false)
    var lastUpdate: Instant = Instant.now()
    
    enum class ChatStatus {
        NEW,
        USER_MESSAGE,
        USER_MESSAGE_PROCESS,
        USER_MESSAGE_PROCESS_COMPLETE,
        SEARCH_PUBMED,
        SEARCH_PUBMED_COMPLETE,
        SEARCH_DB,
        SEARCH_DB_COMPLETE,
        SUMMARY,
        COMPLETE,
        FAILED
    }
    
    @PreUpdate
    fun updateTimestamp() {
        lastUpdate = Instant.now()
    }
}

