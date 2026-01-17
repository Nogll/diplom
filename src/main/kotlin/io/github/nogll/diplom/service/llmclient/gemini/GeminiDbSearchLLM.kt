package io.github.nogll.diplom.service.llmclient.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Schema
import com.google.genai.types.Type
import io.github.nogll.diplom.llm.DbSearchLLM
import io.github.nogll.diplom.service.llmclient.GeminiClientService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["llm.model"], havingValue = "gemini")
class GeminiDbSearchLLM(
    private val geminiClientService: GeminiClientService
) : DbSearchLLM {
    private val client get() = geminiClientService.client
    private val objectMapper = jacksonObjectMapper()

    private val schema = Schema.builder()
        .type(Type.Known.ARRAY)
        .items(Schema.builder().type(Type.Known.BOOLEAN))
        .build()

    override fun process(keywords: List<String>, interactions: List<DbSearchLLM.SearchInteraction>): List<Boolean> {
        if (interactions.isEmpty()) {
            return emptyList()
        }

        val config = GenerateContentConfig.builder()
            .responseMimeType("application/json")
            .responseSchema(schema)
            .candidateCount(1)
            .build()

        val interactionsJson = objectMapper.writeValueAsString(
            interactions.map { mapOf(
                "plant" to it.plant,
                "compound" to it.compound,
                "effects" to it.effects
            ) }
        )

        val prompt = """
            Given the following keywords and list of interactions, determine which interactions are relevant.
            Return a JSON array of booleans: true if the interaction at that index is relevant, false otherwise.
            
            Keywords: ${keywords.joinToString(", ")}
            
            Interactions:
            $interactionsJson
            
            Return an array of ${interactions.size} boolean values.
            """.trimIndent()

        val response = client.models.generateContent(
            "gemini-2.5-flash",
            prompt,
            config
        ).text() ?: throw IllegalStateException("Empty response from LLM")

        val result: List<Boolean> = objectMapper.readValue(response)
        
        // Ensure result size matches interactions size
        return if (result.size == interactions.size) {
            result
        } else {
            // If size mismatch, return all false (conservative approach)
            List(interactions.size) { false }
        }
    }
}
