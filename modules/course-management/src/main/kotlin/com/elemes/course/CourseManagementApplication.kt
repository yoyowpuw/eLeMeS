package com.elemes.course

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableKafka
class CourseManagementApplication

fun main(args: Array<String>) {
    runApplication<CourseManagementApplication>(*args)
}
