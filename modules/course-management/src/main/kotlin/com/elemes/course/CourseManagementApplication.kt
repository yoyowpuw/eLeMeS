package com.elemes.course

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CourseManagementApplication

fun main(args: Array<String>) {
    runApplication<CourseManagementApplication>(*args)
}
