package io.github.nogll.diplom.service.llmclient.openai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "llm.openai")
data class OpenAIClientConfig(
    val apiKey: String,
    val baseUrl: String,
)