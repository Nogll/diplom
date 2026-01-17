package io.github.nogll.diplom.service.proxy

import com.google.genai.GlobalConfig
import io.github.nogll.diplom.dto.ProxyChangeRequest
import io.github.nogll.diplom.service.llmclient.GeminiClientService
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy

@Service
class ChangeProxyServerService(
    val geminiClientService: GeminiClientService,
) {
    fun changeProxy(request: ProxyChangeRequest) {
        val proxy = Proxy(request.type, InetSocketAddress(request.host, request.port))
        GlobalConfig.proxy = proxy

        if (request.username != null && request.password != null) {
            Authenticator.setDefault(MyAuth(request.username, request.password))
        }

        geminiClientService.recreateClient()
    }

    @PostConstruct
    fun init() {
//        changeProxy(ProxyChangeRequest(
//            Proxy.Type.HTTP,
//            "localhost",
//            8888,
//            null,
//            null
//        ))

        changeProxy(ProxyChangeRequest(
            Proxy.Type.SOCKS,
            "localhost",
            10808,
            null,
            null
        ))
    }
}

private class MyAuth(val pass: String, val username: String): Authenticator() {
    override fun getPasswordAuthentication(): PasswordAuthentication {
        return PasswordAuthentication(pass, username.toCharArray())
    }
}