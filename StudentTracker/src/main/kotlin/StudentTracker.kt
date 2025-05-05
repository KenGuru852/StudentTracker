package org.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StudentTracker

fun main(args: Array<String>) {
    runApplication<StudentTracker>(*args)
}
