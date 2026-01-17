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
            You are generating PubMed search queries for scientific literature analysis.
            
            Task:
            Generate 3–5 broad and inclusive PubMed search queries in English that satisfy ALL of the following:
            
            1. The queries MUST focus on substances of plant origin:
               - herbal medicine
               - medicinal plants
               - phytotherapy
               - plant extracts
               - phytochemicals
               - natural compounds from plants
            
            2. The queries MUST relate to the user’s described condition, symptom, or biological effect.
            
            3. The queries MUST NOT mention specific plant species, genera, or compound names
               unless they are explicitly stated in the user query.
            
            4. The goal is to retrieve a wide and unbiased corpus of relevant articles,
               including reviews, experimental studies, and meta-analyses.
            
            User query:
            "$query"
            
            Output requirements:
            - Return ONLY valid JSON
            - Follow the response schema exactly
            - Do NOT include any explanations or additional text
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

