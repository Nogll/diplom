package io.github.nogll.diplom.dto

data class ErrorResponse(
    val success: Boolean = false,
    val message: String
) {
    constructor(msg: String?) : this(false, msg ?: "Error")
}
