package com.elemes.assessment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class AssessmentApplication

fun main(args: Array<String>) {
    runApplication<AssessmentApplication>(*args)
}
