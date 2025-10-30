package io.github.nogll.diplom.controllers

import io.github.nogll.diplom.llm.GeminiService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController(
    val llm: GeminiService
) {
    @GetMapping("/hello")
    fun hello() = "Hello World!"

    @GetMapping("/gen")
    fun generate(@RequestBody msg: String) = llm.generate(msg)
}