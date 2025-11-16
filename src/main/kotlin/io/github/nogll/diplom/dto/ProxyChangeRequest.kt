package io.github.nogll.diplom.dto

import java.net.Proxy

data class ProxyChangeRequest(
    val type: Proxy.Type,
    val host: String,
    val port: Int,
    val password: String?,
    val username: String?,
)
