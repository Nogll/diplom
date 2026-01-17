package io.github.nogll.diplom.repository

import io.github.nogll.diplom.entity.PubMedQuery
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PubMedQueryRepository : JpaRepository<PubMedQuery, Long> {
    fun findByChatId(chatId: UUID): List<PubMedQuery>
}

