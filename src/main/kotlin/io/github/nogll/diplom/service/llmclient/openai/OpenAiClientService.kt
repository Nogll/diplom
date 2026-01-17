package io.github.nogll.diplom.service.llmclient.openai

import com.google.genai.Client
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatModel
import com.openai.models.models.Model
import org.springframework.stereotype.Service

@Service
class OpenAiClientService(
    val config: OpenAIClientConfig,
) {
    companion object {
        val MODEL = ChatModel.GPT_4O
    }

    @Volatile
    private var openAiClient: OpenAIClient? = null

    val client get() = openAiClient ?: recreateClient()

    fun recreateClient(): OpenAIClient {
        val newClient = createClient()
        openAiClient?.close()
        openAiClient = newClient
        return newClient
    }

    fun createClient(): OpenAIClient {
        return OpenAIOkHttpClient.builder()
            .apiKey(config.apiKey)
            .baseUrl(config.baseUrl)
            .build()
    }
}