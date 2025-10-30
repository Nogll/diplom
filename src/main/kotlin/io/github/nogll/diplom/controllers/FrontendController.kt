package io.github.nogll.diplom.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class FrontendController {
    
    @GetMapping("/")
    fun index(): String {
        return "forward:/index.html"
    }
    
    @GetMapping("/app")
    fun app(): String {
        return "forward:/index.html"
    }
}

