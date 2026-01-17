package io.github.nogll.diplom.service.llmclient.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Schema
import com.google.genai.types.Type
import io.github.nogll.diplom.llm.UserQueryLLM
import io.github.nogll.diplom.service.llmclient.GeminiClientService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["llm.model"], havingValue = "gemini")
class GeminiUserQueryLLM(
    private val geminiClientService: GeminiClientService
) : UserQueryLLM {
    private val client get() = geminiClientService.client
    private val objectMapper = jacksonObjectMapper()

    private val schema = Schema.builder()
        .type(Type.Known.OBJECT)
        .required("pubmed_queries", "keywords")
        .properties(
            mapOf(
                "pubmed_queries" to Schema.builder()
                    .type(Type.Known.ARRAY)
                    .items(Schema.builder().type(Type.Known.STRING))
                    .build(),
                "keywords" to Schema.builder()
                    .type(Type.Known.ARRAY)
                    .items(Schema.builder().type(Type.Known.STRING))
                    .build()
            )
        ).build()

    override fun processUserQuery(query: String): UserQueryLLM.SearchQuery {
        val config = GenerateContentConfig.builder()
            .responseMimeType("application/json")
            .responseSchema(schema)
            .candidateCount(1)
            .build()

        val prompt = """
            Analyze the following user query and generate:
            1) pubmed_queries: 3-5 optimized search queries in English for PubMed
            2) keywords: key terms and phrases in English for searching internal database
            
            User query: $query
            
            Return JSON with two arrays: pubmed_queries (search queries for PubMed) and keywords (terms for database search).
            """.trimIndent()

        val response = client.models.generateContent(
            "gemini-2.5-flash",
            prompt,
            config
        ).text() ?: throw IllegalStateException("Empty response from LLM")

        val result: QueryResult = objectMapper.readValue(response)
        
        return UserQueryLLM.SearchQuery(
            pubmedQueries = result.pubmed_queries ?: emptyList(),
            keywords = result.keywords ?: emptyList()
        )
    }

    private data class QueryResult(
        val pubmed_queries: List<String>?,
        val keywords: List<String>?
    )
}
