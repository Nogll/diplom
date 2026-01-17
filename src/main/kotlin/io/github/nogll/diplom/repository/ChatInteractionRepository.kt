package io.github.nogll.diplom.repository

import io.github.nogll.diplom.entity.ChatInteraction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ChatInteractionRepository : JpaRepository<ChatInteraction, Long> {
    @Query("SELECT ci FROM ChatInteraction ci " +
            "LEFT JOIN FETCH ci.interaction i " +
            "LEFT JOIN FETCH i.plant " +
            "LEFT JOIN FETCH i.compound " +
            "LEFT JOIN FETCH i.source s " +
            "LEFT JOIN FETCH s.article " +
            "LEFT JOIN FETCH s.model " +
            "WHERE ci.chat.id = :chatId")
    fun findByChatIdWithRelations(chatId: UUID): List<ChatInteraction>
}

