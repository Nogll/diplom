package io.github.nogll.diplom.service.llmclient.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Schema
import com.google.genai.types.Type
import io.github.nogll.diplom.llm.ArticleProcessingLLM
import io.github.nogll.diplom.service.llmclient.GeminiClientService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["llm.model"], havingValue = "gemini")
class GeminiArticleProcessingLLM(
    private val geminiClientService: GeminiClientService
) : ArticleProcessingLLM {
    private val client get() = geminiClientService.client
    private val objectMapper = jacksonObjectMapper()

    private val schema = Schema.builder()
        .type(Type.Known.ARRAY)
        .items(
            Schema.builder()
                .type(Type.Known.OBJECT)
                .required("plant", "compound", "effects")
                .properties(
                    mapOf(
                        "plant" to Schema.builder().type(Type.Known.STRING).build(),
                        "compound" to Schema.builder().type(Type.Known.STRING).build(),
                        "effects" to Schema.builder()
                            .type(Type.Known.ARRAY)
                            .items(Schema.builder().type(Type.Known.STRING))
                            .build(),
                        "part" to Schema.builder()
                            .type(Type.Known.ARRAY)
                            .items(Schema.builder().type(Type.Known.STRING))
                            .build()
                    )
                )
        ).build()

    override fun process(text: String): List<ArticleProcessingLLM.ProcessedArticle> {
        val config = GenerateContentConfig.builder()
            .responseMimeType("application/json")
            .responseSchema(schema)
            .candidateCount(1)
            .build()

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
            Do not include any text outside the JSON.
            
            Now process the following abstract:
            $text
            """.trimIndent()

        val response = client.models.generateContent(
            "gemini-2.5-flash",
            prompt,
            config
        ).text() ?: throw IllegalStateException("Empty response from LLM")

        val extractedData: List<ExtractedInteraction> = objectMapper.readValue(response)
        
        return extractedData.map { item ->
            ArticleProcessingLLM.ProcessedArticle(
                plant = item.plant,
                compound = item.compound,
                effects = item.effects,
                part = item.part
            )
        }
    }

    private data class ExtractedInteraction(
        val plant: String,
        val compound: String,
        val effects: List<String>,
        val part: List<String>? = null
    )
}
