package io.github.nogll.diplom.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.nogll.diplom.dto.ProcessArticleRequest
import io.github.nogll.diplom.entity.Chat
import io.github.nogll.diplom.entity.ChatInteraction
import io.github.nogll.diplom.entity.PubMedQuery
import io.github.nogll.diplom.llm.ArticleProcessingLLM
import io.github.nogll.diplom.llm.DbSearchLLM
import io.github.nogll.diplom.llm.SummaryLLM
import io.github.nogll.diplom.llm.UserQueryLLM
import io.github.nogll.diplom.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Service
class ChatProcessingService(
    private val userQueryLLM: UserQueryLLM,
    private val articleProcessingLLM: ArticleProcessingLLM,
    private val dbSearchLLM: DbSearchLLM,
    private val summaryLLM: SummaryLLM,
    private val chatRepository: ChatRepository,
    private val pubmedQueryRepository: PubMedQueryRepository,
    private val chatInteractionRepository: ChatInteractionRepository,
    private val articleRepository: ArticleRepository,
    private val interactionRepository: InteractionRepository,
    private val articleProcessingService: ArticleProcessingService,
    private val pubmedService: PubMedService,
    private val modelRepository: ModelRepository,
    private val plantRepository: PlantRepository,
    private val compoundRepository: CompoundRepository,
    private val sourceRepository: SourceRepository
) {
    private val objectMapper = jacksonObjectMapper()
    private val logger = LoggerFactory.getLogger(ChatProcessingService::class.java)
    
    // Configuration limits
    private val maxArticlesPerQuery = 3
    private val maxTotalArticles = 15

    @Transactional
    fun processUserMessage(chatId: UUID, message: String) {
        logger.info("Processing user message for chat $chatId")
        val chat = chatRepository.findById(chatId)
            .orElseThrow { IllegalArgumentException("Chat not found: $chatId") }
        
        chat.userMessage = message
        chat.status = Chat.ChatStatus.USER_MESSAGE  // Worker will pick this up and change to USER_MESSAGE_PROCESS
        chatRepository.save(chat)
        logger.info("User message saved for chat $chatId, status set to USER_MESSAGE")
    }

    @Transactional
    fun processUserMessageAnalysis(chatId: UUID) {
        logger.info("Starting user message analysis for chat $chatId")
        val chat = chatRepository.findById(chatId)
            .orElseThrow { IllegalArgumentException("Chat not found: $chatId") }
        
        if (chat.userMessage == null) {
            throw IllegalStateException("No user message to process")
        }

        try {
            // Update status to processing
            chat.status = Chat.ChatStatus.USER_MESSAGE_PROCESS
            chatRepository.save(chat)
            logger.debug("Chat $chatId status updated to USER_MESSAGE_PROCESS")
            
            logger.info("Calling LLM to analyze user query for chat $chatId")
            val searchQuery = userQueryLLM.processUserQuery(chat.userMessage!!)
            logger.info("LLM returned ${searchQuery.pubmedQueries.size} PubMed queries and ${searchQuery.keywords.size} keywords for chat $chatId")
            
            // Save keywords as JSON
            chat.keywords = objectMapper.writeValueAsString(searchQuery.keywords)
            logger.debug("Keywords saved for chat $chatId: ${searchQuery.keywords}")
            
            // Save PubMed queries
            searchQuery.pubmedQueries.forEachIndexed { index, query ->
                logger.debug("Saving PubMed query ${index + 1}/${searchQuery.pubmedQueries.size} for chat $chatId: $query")
                val pubmedQuery = PubMedQuery().apply {
                    this.chat = chat
                    this.query = query
                }
                pubmedQueryRepository.save(pubmedQuery)
            }
            
            chat.status = Chat.ChatStatus.USER_MESSAGE_PROCESS_COMPLETE
            chatRepository.save(chat)
            logger.info("User message analysis completed for chat $chatId, status set to USER_MESSAGE_PROCESS_COMPLETE")
        } catch (e: Exception) {
            logger.error("Error during user message analysis for chat $chatId", e)
            chat.status = Chat.ChatStatus.FAILED
            chatRepository.save(chat)
            throw e
        }
    }

    fun processPubMedSearch(chatId: UUID) {
        logger.info("Starting PubMed search for chat $chatId")
        var chat = chatRepository.findById(chatId)
            .orElseThrow { IllegalArgumentException("Chat not found: $chatId") }
        
        if (chat.status != Chat.ChatStatus.USER_MESSAGE_PROCESS_COMPLETE) {
            throw IllegalStateException("Chat is not ready for PubMed search")
        }

        try {
            chat.status = Chat.ChatStatus.SEARCH_PUBMED
            chat = chatRepository.save(chat)
            logger.debug("Chat $chatId status updated to SEARCH_PUBMED")

            val queries = pubmedQueryRepository.findByChatId(chatId)
            logger.info("Found ${queries.size} PubMed queries to process for chat $chatId (limit: $maxArticlesPerQuery)")
            
            // Check existing articles by URL to avoid duplicates
            // Note: In production, you might want to add a URL index for better performance
            val allArticles = articleRepository.findAll()
            val existingUrls = allArticles.map { it.url }.toSet()
            logger.debug("Found ${existingUrls.size} existing articles in database")
            
            val articlesToProcess = mutableListOf<Pair<String, String>>() // (title, url)

            // Search PubMed and collect articles
            var queryIndex = 0
            for (query in queries) {
                queryIndex++
                if (articlesToProcess.size >= maxTotalArticles) {
                    logger.debug("Reached max total articles limit ($maxTotalArticles) for chat $chatId")
                    break
                }
                
                logger.info("Searching PubMed with query $queryIndex/${queries.size}: ${query.query}")
                val articles = pubmedService.searchArticles(query.query, 1).take(maxArticlesPerQuery)
                logger.info("Found ${articles.size} articles for query: ${query.query}")
                
                var newArticlesCount = 0
                for (article in articles) {
                    if (articlesToProcess.size >= maxTotalArticles) break
                    
                    // Skip if article already exists
                    if (!existingUrls.contains(article.url)) {
                        articlesToProcess.add(Pair(article.title, article.url))
                        newArticlesCount++
                        logger.debug("Added new article to process: ${article.title}")
                    } else {
                        logger.debug("Skipped existing article: ${article.title}")
                    }
                }
                logger.info("Added $newArticlesCount new articles from query: ${query.query}")
            }

            logger.info("Total articles to process for chat $chatId: ${articlesToProcess.size}")

            // Process each article
            var processedCount = 0
            var skippedCount = 0
            for ((index, article) in articlesToProcess.withIndex()) {
                logger.info("Processing article ${index + 1}/${articlesToProcess.size}: ${article.first}")
                
                val abstract = pubmedService.getArticleAbstract(article.second)
                if (abstract == null) {
                    logger.warn("Could not fetch abstract for article: ${article.first}, skipping")
                    skippedCount++
                    continue
                }
                
                // Process article using ArticleProcessingService
                val request = ProcessArticleRequest(
                    url = article.second,
                    title = article.first,
                    abstract = abstract
                )
                try {
                    articleProcessingService.processArticle(request)
                    Thread.sleep(20.seconds.toJavaDuration())
                } catch (e: Exception) {
                    logger.warn("Failed process article ${request.url}, skipping", e)
                    skippedCount++
                    continue
                }
                processedCount++
                logger.info("Successfully processed article ${index + 1}/${articlesToProcess.size}: ${article.first}")
            }

            logger.info("PubMed search completed for chat $chatId: processed $processedCount articles, skipped $skippedCount")
            chat.status = Chat.ChatStatus.SEARCH_PUBMED_COMPLETE
            chat = chatRepository.save(chat)
            logger.info("Chat $chatId status updated to SEARCH_PUBMED_COMPLETE")
        } catch (e: Exception) {
            logger.error("Error during PubMed search for chat $chatId", e)
            chat.status = Chat.ChatStatus.FAILED
            chatRepository.save(chat)
            throw e
        }
    }

    @Transactional
    fun processDbSearch(chatId: UUID) {
        logger.info("Starting DB search for chat $chatId")
        val chat = chatRepository.findById(chatId)
            .orElseThrow { IllegalArgumentException("Chat not found: $chatId") }
        
        if (chat.status != Chat.ChatStatus.SEARCH_PUBMED_COMPLETE) {
            throw IllegalStateException("Chat is not ready for DB search")
        }

        try {
            chat.status = Chat.ChatStatus.SEARCH_DB
            chatRepository.save(chat)
            logger.debug("Chat $chatId status updated to SEARCH_DB")

            val keywordsJson = chat.keywords
                ?: throw IllegalStateException("Keywords not found")
            val keywords: List<String> = objectMapper.readValue(
                keywordsJson,
                object : TypeReference<List<String>>(){}
            )
            logger.info("Loaded ${keywords.size} keywords for DB search in chat $chatId: $keywords")

            // Load interactions in batches
            logger.info("Loading all interactions from database for chat $chatId")
            val allInteractions = interactionRepository.findAllWithRelations()
            logger.info("Loaded ${allInteractions.size} total interactions from database")
            
            // Convert to SearchInteraction format
            val searchInteractions = allInteractions.map { interaction ->
                DbSearchLLM.SearchInteraction(
                    plant = interaction.plant.name,
                    compound = interaction.compound.name ?: "",
                    effects = interaction.getEffectsList()
                )
            }
            logger.debug("Converted ${searchInteractions.size} interactions to search format")

            // Use LLM to filter relevant interactions
            logger.info("Calling LLM to evaluate relevance of ${searchInteractions.size} interactions for chat $chatId")
            val relevanceFlags = dbSearchLLM.process(keywords, searchInteractions)
            logger.info("LLM evaluated ${relevanceFlags.size} interactions for chat $chatId")

            // Save relevant interactions
            var relevantCount = 0
            for (i in searchInteractions.indices) {
                if (relevanceFlags.getOrNull(i) == true) {
                    val interaction = allInteractions[i]
                    val chatInteraction = ChatInteraction().apply {
                        this.chat = chat
                        this.interaction = interaction
                    }
                    chatInteractionRepository.save(chatInteraction)
                    relevantCount++
                    logger.debug("Saved relevant interaction $relevantCount: ${interaction.plant.name} - ${interaction.compound.name}")
                }
            }

            logger.info("DB search completed for chat $chatId: found $relevantCount relevant interactions out of ${allInteractions.size} total")
            chat.status = Chat.ChatStatus.SEARCH_DB_COMPLETE
            chatRepository.save(chat)
            logger.info("Chat $chatId status updated to SEARCH_DB_COMPLETE")
        } catch (e: Exception) {
            logger.error("Error during DB search for chat $chatId", e)
            chat.status = Chat.ChatStatus.FAILED
            chatRepository.save(chat)
            throw e
        }
    }

    @Transactional
    fun generateSummary(chatId: UUID) {
        logger.info("Starting summary generation for chat $chatId")
        val chat = chatRepository.findById(chatId)
            .orElseThrow { IllegalArgumentException("Chat not found: $chatId") }
        
        if (chat.status != Chat.ChatStatus.SEARCH_DB_COMPLETE) {
            throw IllegalStateException("Chat is not ready for summary generation")
        }

        try {
            chat.status = Chat.ChatStatus.SUMMARY
            chatRepository.save(chat)
            logger.debug("Chat $chatId status updated to SUMMARY")

            val userMessage = chat.userMessage
                ?: throw IllegalStateException("User message not found")
            logger.debug("User message for summary: $userMessage")
            
            val chatInteractions = chatInteractionRepository.findByChatIdWithRelations(chatId)
            logger.info("Found ${chatInteractions.size} relevant interactions to include in summary for chat $chatId")
            
            val summaryInteractions = chatInteractions.map { chatInteraction ->
                val interaction = chatInteraction.interaction
                SummaryLLM.SummaryInteraction(
                    plant = interaction.plant.name,
                    compound = interaction.compound.name ?: "",
                    effects = interaction.getEffectsList(),
                    part = interaction.getPlantPartsList()
                )
            }
            logger.debug("Converted ${summaryInteractions.size} interactions to summary format")

            logger.info("Calling LLM to generate summary for chat $chatId with ${summaryInteractions.size} interactions")
            val summaryText = summaryLLM.summarize(userMessage, summaryInteractions)
            logger.info("Summary generated for chat $chatId (length: ${summaryText.length} characters)")
            
            chat.summary = summaryText
            chat.status = Chat.ChatStatus.COMPLETE
            chatRepository.save(chat)
            logger.info("Chat $chatId processing completed successfully, status set to COMPLETE")
        } catch (e: Exception) {
            logger.error("Error during summary generation for chat $chatId", e)
            chat.status = Chat.ChatStatus.FAILED
            chatRepository.save(chat)
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun getChat(chatId: UUID): Chat {
        return chatRepository.findById(chatId)
            .orElseThrow { IllegalArgumentException("Chat not found: $chatId") }
    }

    @Transactional
    fun createChat(): Chat {
        logger.info("Creating new chat")
        val chat = Chat()
        val savedChat = chatRepository.save(chat)
        logger.info("New chat created with ID: ${savedChat.id}")
        return savedChat
    }
}

