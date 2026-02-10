package io.github.nogll.diplom.service.llmclient.openai

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.openai.core.JsonSchemaLocalValidation
import com.openai.models.ChatModel
import com.openai.models.ResponseFormatJsonSchema
import com.openai.models.chat.completions.ChatCompletionCreateParams
import io.github.nogll.diplom.llm.DbSearchLLM
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.asSequence
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Service
@ConditionalOnProperty(name = ["llm.model"], havingValue = "openai")
class OpenAiDbSearchLLM(
    val clientService: OpenAiClientService
) : DbSearchLLM {
    private val objectMapper = jacksonObjectMapper()

    override fun process(keywords: List<String>, interactions: List<DbSearchLLM.SearchInteraction>): List<Boolean> {
        if (interactions.isEmpty()) {
            return emptyList()
        }

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
            
            Return JSON { "results" : [1, 0, 1, ...]} an array of ${interactions.size} 0 or 1 values.
            Return only valid JSON according to the schema.
            Do not include any text outside the JSON.
            """.trimIndent()

        data class Response(
            val results: List<Int>
        )

        val params = ChatCompletionCreateParams.builder()
            .addUserMessage(prompt)
            .model(OpenAiClientService.MODEL)
            .responseFormat(Response::class.java, JsonSchemaLocalValidation.NO)
            .build()

        Thread.sleep(15.seconds.toJavaDuration())
        val result = clientService.client.chat().completions().create(params).choices().asSequence()
            .flatMap { it.message().content().asSequence() }
            .firstOrNull()
            ?: throw IllegalStateException("Empty response from LLM")

        // Ensure result size matches interactions size
        return if (result.results.size == interactions.size) {
            result.results.map { it == 1 }
        } else {
            // If size mismatch, return all false (conservative approach)
            List(interactions.size) { false }
        }
    }
}

