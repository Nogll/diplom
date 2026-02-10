package io.github.nogll.diplom.dto

import java.time.Instant
import java.util.*

data class ChatResponse(
    val status: String,
    val userMessage: String?,
    val queries: List<PubMedQueryDto>?,
    val interactions: List<ChatInteractionDto>?,
    val summary: ChatSummaryDto?,
    val lastUpdate: Instant
)

data class PubMedQueryDto(
    val query: String
)

data class ChatInteractionDto(
    val id: Long,
    val plantName: String,
    val compoundName: String,
    val effects: List<String>,
    val plantParts: List<String>?,
    val article: ArticleInfoDto
)

data class ArticleInfoDto(
    val title: String,
    val url: String
)

data class ChatSummaryDto(
    val text: String
)

data class ChatMessageRequest(
    val message: String
)

data class ChatListItemDto(
    val id: UUID,
    val userMessage: String?,
    val status: String
)

data class ChatListResponse(
    val chats: List<ChatListItemDto>
)
