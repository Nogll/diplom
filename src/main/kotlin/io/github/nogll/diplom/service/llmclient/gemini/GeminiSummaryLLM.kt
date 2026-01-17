package io.github.nogll.diplom.service.llmclient.gemini

import com.google.genai.types.GenerateContentConfig
import io.github.nogll.diplom.llm.SummaryLLM
import io.github.nogll.diplom.service.llmclient.GeminiClientService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["llm.model"], havingValue = "gemini")
class GeminiSummaryLLM(
    private val geminiClientService: GeminiClientService
) : SummaryLLM {
    private val client get() = geminiClientService.client

    override fun summarize(userQuery: String, interactions: List<SummaryLLM.SummaryInteraction>): String {
        val config = GenerateContentConfig.builder()
            .candidateCount(1)
            .build()

        val interactionsText = interactions.joinToString("\n") { interaction ->
            "- Plant: ${interaction.plant}, Compound: ${interaction.compound}, " +
            "Effects: ${interaction.effects.joinToString(", ")}, " +
            "Parts: ${interaction.part?.joinToString(", ") ?: "N/A"}"
        }

        val prompt = """
            Based on the user's query and the relevant interactions found, provide a comprehensive summary.
            
            User query: $userQuery
            
            Relevant interactions found:
            $interactionsText
            
            Provide a clear, structured summary that addresses the user's query using the information from these interactions.
            Include relevant details about plants, compounds, and their effects.
            """.trimIndent()

        return client.models.generateContent(
            "gemini-2.5-flash",
            prompt,
            config
        ).text() ?: throw IllegalStateException("Empty response from LLM")
    }
}
