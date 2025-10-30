package io.github.nogll.diplom.dto

import org.springframework.data.domain.Page

data class InteractionResponse(
    val content: List<InteractionDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val size: Int
) {
    companion object {
        fun fromPage(page: org.springframework.data.domain.Page<InteractionDto>): InteractionResponse {
            return InteractionResponse(
                content = page.content,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                currentPage = page.number,
                size = page.size
            )
        }
    }
}

