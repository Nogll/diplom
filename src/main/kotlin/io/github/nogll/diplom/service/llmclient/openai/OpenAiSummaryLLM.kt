package io.github.nogll.diplom.service.llmclient.openai

import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletionCreateParams
import io.github.nogll.diplom.llm.SummaryLLM
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.asSequence

@Service
@ConditionalOnProperty(name = ["llm.model"], havingValue = "openai")
class OpenAiSummaryLLM(
    val clientService: OpenAiClientService
) : SummaryLLM {
    
    override fun summarize(userQuery: String, interactions: List<SummaryLLM.SummaryInteraction>): String {
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

        val params = ChatCompletionCreateParams.builder()
            .addUserMessage(prompt)
            .model(OpenAiClientService.MODEL)
            .build()

        val response = clientService.client.chat().completions().create(params)
        
        return response.choices().asSequence()
            .flatMap { it.message().content().asSequence() }
            .joinToString("")
    }
}

