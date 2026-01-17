package io.github.nogll.diplom

import io.github.nogll.diplom.service.llmclient.openai.OpenAIClientConfig
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(OpenAIClientConfig::class)
class DiplomApplication

fun main(args: Array<String>) {
    runApplication<DiplomApplication>(*args)
}
