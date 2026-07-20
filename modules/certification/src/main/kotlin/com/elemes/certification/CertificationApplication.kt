package com.elemes.certification

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableKafka
class CertificationApplication

fun main(args: Array<String>) {
    runApplication<CertificationApplication>(*args)
}
