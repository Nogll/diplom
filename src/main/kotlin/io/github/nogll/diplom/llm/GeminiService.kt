package io.github.nogll.diplom.llm

import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Schema
import com.google.genai.types.ThinkingConfig
import com.google.genai.types.Type
import org.springframework.stereotype.Service

@Service
class GeminiService {
    private val client: Client by lazy { Client() }
    private val schema = Schema.builder()
        .type(Type.Known.ARRAY)
        .items(
            Schema.builder()
                .type(Type.Known.OBJECT)
                .required("plant", "compound", "effects")
                .properties(mapOf(
                    "plant" to Schema.builder().type(Type.Known.STRING).build(),
                    "compound" to Schema.builder().type(Type.Known.STRING).build(),
                    "effects" to Schema.builder().type(Type.Known.ARRAY)
                        .items(Schema.builder().type(Type.Known.STRING)).build(),
                    "part" to Schema.builder().type(Type.Known.ARRAY)
                        .items(Schema.builder().type(Type.Known.STRING))
                        .build(),

                ))
        ).build()

    fun generate(text: String): String {
        val config = GenerateContentConfig.builder()
            .responseMimeType("application/json")
            .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0))
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
            
            Example input:
            "Curcuma longa (turmeric) contains curcumin, which shows anti-inflammatory and antioxidant effects by inhibiting COX-2 and scavenging free radicals."
            
            Example output:
            [
              {
                "plant": "Curcuma longa",
                "compound": "curcumin",
                "effects": ["anti-inflammatory", "antioxidant", "inhibits COX-2", "scavenges free radicals"],
                "part": []
              }
            ]
            
            Now process the following abstract:
            $text
            """.trimIndent()

        return client.models.generateContent(
            "gemini-2.5-flash",
            prompt,
            config
        ).text() ?: "Error, text is empty"
    }

}