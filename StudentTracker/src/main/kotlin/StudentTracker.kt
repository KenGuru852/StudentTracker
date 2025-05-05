package org.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class StudentTracker

fun main(args: Array<String>) {
    runApplication<StudentTracker>(*args)
}

@RestController
@RequestMapping("/api")
class HelloController {

    @GetMapping("/hello")
    fun sayHello(): String {
        return "Привет из Spring Boot и Kotlin!"
    }

    @GetMapping("/user")
    fun getUser(): Map<String, Any> {
        return mapOf(
            "id" to 1,
            "name" to "Иван Иванов",
            "email" to "ivan@example.com"
        )
    }
}