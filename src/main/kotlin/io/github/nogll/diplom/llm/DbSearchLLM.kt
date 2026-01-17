package io.github.nogll.diplom.llm

interface DbSearchLLM {
    data class SearchInteraction(
        val plant: String,
        val compound: String,
        val effects: List<String>,
    )

    fun process(keywords: List<String>, interactions: List<SearchInteraction>): List<Boolean>
}