package io.github.nogll.diplom.service

import io.github.nogll.diplom.entity.Chat
import io.github.nogll.diplom.repository.ChatRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.lang.IllegalStateException
import java.util.*

@Component
class ChatWorker(
    private val chatRepository: ChatRepository,
    private val chatProcessingService: ChatProcessingService
) {
    private val logger = LoggerFactory.getLogger(ChatWorker::class.java)

    @Scheduled(fixedDelay = 10000)
    fun processChats() {
        try {
            // Process user message analysis
            val chatsToAnalyze = chatRepository.findByStatusOrderByLastUpdateAsc(Chat.ChatStatus.USER_MESSAGE)
            chatsToAnalyze.forEach { chat ->
                try {
                    logger.info("Processing user message analysis for chat ${chat.id}")
                    chat.id?.let { id -> chatProcessingService.processUserMessageAnalysis(id) }
                } catch (e: Exception) {
                    failChat(chat)
                    logger.error("Error processing user message analysis for chat ${chat.id}", e)
                }
            }

            // Process PubMed search
            val chatsToSearchPubMed = chatRepository.findByStatusOrderByLastUpdateAsc(Chat.ChatStatus.USER_MESSAGE_PROCESS_COMPLETE)
            chatsToSearchPubMed.forEach { chat ->
                try {
                    logger.info("Processing PubMed search for chat ${chat.id}")
                    chat.id?.let { id -> chatProcessingService.processPubMedSearch(id) }
                } catch (e: Exception) {
                    failChat(chat)
                    logger.error("Error processing PubMed search for chat ${chat.id}", e)
                }
            }

            // Process DB search
            val chatsToSearchDb = chatRepository.findByStatusOrderByLastUpdateAsc(Chat.ChatStatus.SEARCH_PUBMED_COMPLETE)
            chatsToSearchDb.forEach { chat ->
                try {
                    logger.info("Processing DB search for chat ${chat.id}")
                    chat.id?.let { id -> chatProcessingService.processDbSearch(id) }
                } catch (e: Exception) {
                    failChat(chat)
                    logger.error("Error processing DB search for chat ${chat.id}", e)
                }
            }

            // Generate summary
            val chatsToSummarize = chatRepository.findByStatusOrderByLastUpdateAsc(Chat.ChatStatus.SEARCH_DB_COMPLETE)
            chatsToSummarize.forEach { chat ->
                try {
                    logger.info("Generating summary for chat ${chat.id}")
                    chat.id?.let { id -> chatProcessingService.generateSummary(id) }
                } catch (e: Exception) {
                    failChat(chat)
                    logger.error("Error generating summary for chat ${chat.id}", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Error in chat worker", e)
        }
    }

    private fun failChat(chat: Chat) {
        chat.id?.let { id ->
            chatRepository.findById(id).ifPresent { failedChat ->
                failedChat.status = Chat.ChatStatus.FAILED
                chatRepository.save(failedChat)
            }
        }
    }
}

