package io.github.nogll.diplom.service.llmclient.openai

import com.openai.core.JsonSchemaLocalValidation
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletionCreateParams
import io.github.nogll.diplom.llm.UserQueryLLM
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.asSequence

@Service
@ConditionalOnProperty(name = ["llm.model"], havingValue = "openai")
class OpenAiUserQueryLLM(
    val clientService: OpenAiClientService
) : UserQueryLLM {
    override fun processUserQuery(query: String): UserQueryLLM.SearchQuery {
        val prompt = """
            You are generating PubMed search queries and keywords for scientific literature analysis on plant-based bioactive compounds.
            
            Task:
            Generate 2 outputs based on the user query:
            1. PubMed search queries (5-10 queries)
            2. Semantic keywords for database matching
            
            REQUIREMENTS:
            
            PART 1: PubMed Search Queries
            ================================
            - Generate 5-10 PubMed search queries in English
            - Focus on substances of plant origin:
              * herbal medicine, medicinal plants, phytotherapy
              * plant extracts, phytochemicals, natural compounds
              * botanical preparations, traditional medicine
            - Include relevant disease/condition terms from user query
            - DO NOT include specific plant/compound names unless explicitly mentioned by user
            - Use MeSH terms and Boolean operators appropriately
            - Prioritize queries that will retrieve relevant reviews and clinical studies
            
            Example valid PubMed queries:
            - "(plant extracts OR phytochemicals) AND diabetes mellitus AND clinical trial"
            - "herbal medicine AND inflammation AND molecular mechanisms"
            - "medicinal plants AND antioxidant AND oxidative stress"
            
            PART 2: Keywords for Database Matching
            ======================================
            - Extract 5-10 semantic keywords from the user query
            - These keywords will be used for semantic similarity matching with database records
            - Include both BROAD and SPECIFIC concepts:
              * Disease/symptom terms (e.g., "inflammation", "neuroprotection")
              * Biological mechanisms (e.g., "apoptosis inhibition", "enzyme modulation")
              * Plant-related terms (e.g., "botanical", "phytochemical")
              * Therapeutic effects (e.g., "analgesic", "hepatoprotective")
            - Format: simple keywords or short phrases (1 sentence max)
            - Prioritize terms that appear in scientific literature
            - DO NOT include PubMed-specific syntax for keyword (AND, OR, brackets)
            
            User query:
            "$query"
            
            Output requirements:
            - Return ONLY valid JSON
            - Follow the response schema exactly
            - Do NOT include any explanations or additional text
            
            Response schema:
            data class Response(
                val pubmed_queries: List<String>,
                val keywords: List<String>
            )
            """.trimIndent()



        data class Response(
            val pubmed_queries: List<String>,
            val keywords: List<String>
        )

        val params = ChatCompletionCreateParams.builder()
            .addUserMessage(prompt)
            .model(OpenAiClientService.MODEL)
            .responseFormat(Response::class.java, JsonSchemaLocalValidation.NO)
            .build()

        val result = clientService.client.chat().completions().create(params).choices().asSequence()
            .flatMap { it.message().content().asSequence() }
            .firstOrNull()
            ?: throw IllegalStateException("Empty response from LLM")

        return UserQueryLLM.SearchQuery(
            pubmedQueries = result.pubmed_queries,
            keywords = result.keywords
        )
    }
}

