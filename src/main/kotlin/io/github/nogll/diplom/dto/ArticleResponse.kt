package io.github.nogll.diplom.dto

data class ArticleResponse(
    val content: List<ArticleDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val size: Int
) {
    companion object {
        fun fromPage(page: org.springframework.data.domain.Page<ArticleDto>): ArticleResponse {
            return ArticleResponse(
                content = page.content,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                currentPage = page.number,
                size = page.size
            )
        }
    }
}
