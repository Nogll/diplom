package io.github.nogll.diplom.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "chat_interactions")
class ChatInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    lateinit var chat: Chat
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interaction_id", nullable = false)
    lateinit var interaction: Interaction
}

