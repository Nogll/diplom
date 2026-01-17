package io.github.nogll.diplom.llm

interface SummaryLLM {
    data class SummaryInteraction(
        val plant: String,
        val compound: String,
        val effects: List<String>,
        val part: List<String>? = null,
    )

    fun summarize(userQuery: String, interactions: List<SummaryInteraction>): String
}