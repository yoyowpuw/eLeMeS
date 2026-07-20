package com.elemes.enrollment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableKafka
@EnableScheduling
class AssignmentEnrollmentApplication

fun main(args: Array<String>) {
    runApplication<AssignmentEnrollmentApplication>(*args)
}
