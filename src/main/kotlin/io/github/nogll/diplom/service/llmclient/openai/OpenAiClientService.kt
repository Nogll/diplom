package io.github.nogll.diplom.service.llmclient.openai

import com.openai.client.OpenAIClient
import com.openai.client.OpenAIClientImpl
import com.openai.core.ClientOptions
import com.openai.models.ChatModel
import io.github.nogll.diplom.openaiclien.OkHttpClient
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

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
//        return OpenAIOkHttpClient.builder()
//            .timeout(30.seconds.toJavaDuration())
//            .apiKey(config.apiKey)
//            .maxRetries(4)
//            .baseUrl(config.baseUrl)
//            .build()
        val clientOptions = ClientOptions.builder()
        clientOptions.timeout(30.seconds.toJavaDuration())
        clientOptions.apiKey(config.apiKey)
        clientOptions.maxRetries(4)
        clientOptions.baseUrl(config.baseUrl)
        return OpenAIClientImpl(
            clientOptions
                .httpClient(
                    OkHttpClient.builder()
                        .timeout(clientOptions.timeout())
                        .build()
                )
                .build()
        )
    }
}