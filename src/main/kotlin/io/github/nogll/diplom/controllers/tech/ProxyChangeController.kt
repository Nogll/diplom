package io.github.nogll.diplom.controllers.tech

import io.github.nogll.diplom.dto.ProxyChangeRequest
import io.github.nogll.diplom.service.proxy.ChangeProxyServerService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tech")
class ProxyChangeController(
    val service: ChangeProxyServerService
) {
    @PostMapping("/proxy")
    fun changeProxy(@RequestBody request: ProxyChangeRequest) {
        service.changeProxy(request)
    }
}