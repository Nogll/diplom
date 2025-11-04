package io.github.nogll.diplom.dto

data class ProcessArticleResponse(
    val success: Boolean = true,
    val message: String,
    val interactionsCount: Int
) {
    constructor(count: Int) : this(true, "Ok", count)
}
