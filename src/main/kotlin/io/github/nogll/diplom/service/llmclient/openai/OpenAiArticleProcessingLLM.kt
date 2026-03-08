package io.github.nogll.diplom.service.llmclient.openai

import com.openai.core.JsonSchemaLocalValidation
import com.openai.models.chat.completions.ChatCompletionCreateParams
import io.github.nogll.diplom.llm.ArticleProcessingLLM
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.*
import kotlin.jvm.optionals.asSequence
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Service
@ConditionalOnProperty(name = ["llm.model"], havingValue = "openai")
class OpenAiArticleProcessingLLM(
    val clientService: OpenAiClientService
) : ArticleProcessingLLM {
    override fun process(text: String): List<ArticleProcessingLLM.ProcessedArticle> {
        val prompt = """
            You are an expert biomedical text miner. 
            Analyze the following scientific abstract and extract structured information about plant-derived bioactive compounds.
            
            For each relationship you find, output an object with the following fields:
            - "plant": the plant species or genus mentioned.
            - "compound": the bioactive chemical or molecule derived from the plant.
            - "effects": an array of biological or pharmacological effects, mechanisms of action, or interactions mentioned (for example: "anti-inflammatory", "reduces oxidative stress", "activates CB1 receptor", "inhibits COX-2").
            - "part": (optional) an array of plant parts mentioned (e.g., "root", "leaf", "seed").
            
            Each item should describe a specific relationship between a plant, its compound, and one or more effects.
            
            Return only valid JSON according to the schema. 
            Start JSON as dictionary with key 'interactions' and then include interactions
            { "interactions" : [...] )
            
            Do not include any text outside the JSON.
            The system is backend server working without user with structured output from LLM.
            The raw result will be passed to the parser without any changes, and it should not fail.
            DO NOT begin with ```json, etc., return ONLY json string with { 
            
            Now process the following abstract:
            $text
            """.trimIndent()

        //StructuredChatCompletionCreateParams
        data class Response(
            @JvmField
            val interactions: List<ExtractedInteraction>
        )

        Thread.sleep(15.seconds.toJavaDuration())
        val params = ChatCompletionCreateParams.builder()
            .addUserMessage(prompt)
            .model(OpenAiClientService.MODEL)
            .temperature(0.0)
            .responseFormat(Response::class.java, JsonSchemaLocalValidation.NO)
            .build()

        val response = clientService.client.chat().completions().create(params).choices()

        return response.asSequence()
            .flatMap { it.message().content().asSequence() }
            .flatMap { it.interactions.asSequence() }
            .map { item ->
                ArticleProcessingLLM.ProcessedArticle(
                    plant = item.plant,
                    compound = item.compound,
                    effects = item.effects,
                    part = item.part.orElse(null)
                )
            }
            .toList()
    }

    private data class ExtractedInteraction(
        @JvmField
        var plant: String,
        @JvmField
        var compound: String,
        @JvmField
        var effects: List<String>,
        @JvmField
        var part: Optional<List<String>>
    )
}