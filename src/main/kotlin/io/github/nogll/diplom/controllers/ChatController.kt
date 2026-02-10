package io.github.nogll.diplom.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.nogll.diplom.dto.*
import io.github.nogll.diplom.entity.Chat
import io.github.nogll.diplom.repository.ChatInteractionRepository
import io.github.nogll.diplom.repository.ChatRepository
import io.github.nogll.diplom.repository.PubMedQueryRepository
import io.github.nogll.diplom.service.ChatProcessingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import java.util.*

@Controller
@RequestMapping("/chat")
class ChatController(
    private val chatProcessingService: ChatProcessingService,
    private val pubmedQueryRepository: PubMedQueryRepository,
    private val chatInteractionRepository: ChatInteractionRepository
) {
    @GetMapping
    fun getChatListPage(): String {
        return "forward:/chat.html"
    }
    
    @GetMapping("/new")
    fun createChat(): RedirectView {
        val chat = chatProcessingService.createChat()
        return RedirectView("/chat/${chat.id}")
    }

    @GetMapping("/{id}")
    fun getChatPage(@PathVariable id: UUID): String {
        return "forward:/chat.html"
    }
}

@RestController
@RequestMapping("/api/v1/chat")
class ChatApiController(
    private val chatProcessingService: ChatProcessingService,
    private val chatRepository: ChatRepository,
    private val pubmedQueryRepository: PubMedQueryRepository,
    private val chatInteractionRepository: ChatInteractionRepository
) {
    private val objectMapper = jacksonObjectMapper()

    @GetMapping
    fun getAllChats(): ResponseEntity<ChatListResponse> {
        val chats = chatRepository.findAllOrderByLastUpdateDesc()
        val chatDtos = chats.map { chat ->
            val userMessage = chat.userMessage?.let { message ->
                if (message.length > 100) {
                    message.substring(0, 100) + "..."
                } else {
                    message
                }
            }
            ChatListItemDto(
                id = chat.id!!,
                userMessage = userMessage,
                status = chat.status.name
            )
        }
        return ResponseEntity.ok(ChatListResponse(chats = chatDtos))
    }

    @GetMapping("/{id}")
    fun getChat(@PathVariable id: UUID): ResponseEntity<ChatResponse> {
        val chat = chatProcessingService.getChat(id)

        val queries = if (chat.status.ordinal >= Chat.ChatStatus.USER_MESSAGE_PROCESS_COMPLETE.ordinal) {
            pubmedQueryRepository.findByChatId(id).map { PubMedQueryDto(query = it.query) }
        } else {
            null
        }

        val interactions = if (chat.status.ordinal >= Chat.ChatStatus.SEARCH_DB_COMPLETE.ordinal) {
            chatInteractionRepository.findByChatIdWithRelations(id).map { chatInteraction ->
                val interaction = chatInteraction.interaction
                ChatInteractionDto(
                    id = interaction.id,
                    plantName = interaction.plant.name,
                    compoundName = interaction.compound.name ?: "",
                    effects = interaction.getEffectsList(),
                    plantParts = interaction.getPlantPartsList(),
                    article = ArticleInfoDto(
                        title = interaction.source.article.title,
                        url = interaction.source.article.url
                    )
                )
            }
        } else {
            null
        }

        val summary = if (chat.status == Chat.ChatStatus.COMPLETE && chat.summary != null) {
            ChatSummaryDto(text = chat.summary!!)
        } else {
            null
        }

        val response = ChatResponse(
            status = chat.status.name,
            userMessage = chat.userMessage,
            queries = queries,
            interactions = interactions,
            summary = summary,
            lastUpdate = chat.lastUpdate
        )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{id}/message")
    fun sendMessage(
        @PathVariable id: UUID,
        @RequestBody request: ChatMessageRequest
    ): ResponseEntity<Map<String, String>> {
        chatProcessingService.processUserMessage(id, request.message)
        return ResponseEntity.ok(mapOf("status" to "ok"))
    }

    @DeleteMapping("/{id}")
    fun deleteChat(@PathVariable id: UUID): ResponseEntity<Map<String, String>> {
        try {
            chatProcessingService.deleteChat(id)
            return ResponseEntity.ok(mapOf("status" to "ok", "message" to "Chat deleted successfully"))
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("status" to "error", "message" to (e.message ?: "Chat not found")))
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("status" to "error", "message" to "Failed to delete chat: ${e.message}"))
        }
    }
}

