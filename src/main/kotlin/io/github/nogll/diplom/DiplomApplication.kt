package io.github.nogll.diplom

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DiplomApplication

fun main(args: Array<String>) {
	runApplication<DiplomApplication>(*args)
}
