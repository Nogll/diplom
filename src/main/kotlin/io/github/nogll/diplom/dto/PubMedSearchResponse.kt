package io.github.nogll.diplom.dto

data class PubMedSearchResponse(
    val articles: List<PubMedArticleDto>,
    val totalPages: Int,
    val currentPage: Int,
    val hasMore: Boolean
)

