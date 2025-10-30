package io.github.nogll.diplom.dto

data class InteractionDto(
    val id: Long,
    val plant: String,
    val compound: String,
    val effects: List<String>,
    val plantParts: List<String>?,
    val model: String? = null,
    val articleTitle: String? = null
)

