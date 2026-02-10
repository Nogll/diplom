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
            You are an assistant summarizing information from a curated scientific database.
            
            RULES:
            - Use ONLY the information provided in "Relevant interactions found".
            - Do NOT introduce external knowledge or assumptions.
            - Do NOT invent compounds, plants, mechanisms, dosages, or formulations.
            - You MAY reframe the user's question to match the available data.
            - If the user's request goes beyond the available information,
              respond with a constrained, data-based explanation of what IS known.
            
            User query:
            "$userQuery"
            
            Relevant interactions found:
            $interactionsText
            
            Task:
            Provide a structured summary that is useful to the user,
            while remaining strictly grounded in the provided data.
            
            Guidelines:
            - Focus on reported associations between plants, compounds, and effects.
            - Clearly distinguish between "reported effects" and "data limitations".
            - Do NOT give medical recommendations or preparation instructions.
            
            Tone:
            Scientific, informative, neutral.
            
            Output:
            A clear textual summary that helps the user understand
            what can and cannot be inferred from the available data.
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

