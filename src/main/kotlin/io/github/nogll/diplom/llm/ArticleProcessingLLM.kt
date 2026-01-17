package io.github.nogll.diplom.llm

interface ArticleProcessingLLM {
    data class ProcessedArticle(
        val plant: String,
        val compound: String,
        val effects: List<String>,
        val part: List<String>? = null,
    )

    fun process(text: String): List<ProcessedArticle>
}