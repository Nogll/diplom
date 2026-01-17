package io.github.nogll.diplom.repository

import io.github.nogll.diplom.entity.Chat
import io.github.nogll.diplom.entity.Chat.ChatStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ChatRepository : JpaRepository<Chat, UUID> {
    @Query("SELECT c FROM Chat c WHERE c.status = :status ORDER BY c.lastUpdate ASC")
    fun findByStatusOrderByLastUpdateAsc(status: ChatStatus): List<Chat>
}

