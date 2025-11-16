package io.github.nogll.diplom.service.llmclient

import com.google.genai.Client
import org.springframework.stereotype.Service

@Service
class GeminiClientService {
    @Volatile
    private var geminiClient: Client? = null

    val client get() = geminiClient ?: recreateClient()

    fun recreateClient() = Client().also { client ->
        geminiClient?.close()
        geminiClient = client
    }
}