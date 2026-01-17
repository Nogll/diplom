package io.github.nogll.diplom.llm

interface UserQueryLLM {
    data class SearchQuery(
        val pubmedQueries: List<String>,
        val keywords: List<String>,
    )

    fun processUserQuery(query: String): SearchQuery
}