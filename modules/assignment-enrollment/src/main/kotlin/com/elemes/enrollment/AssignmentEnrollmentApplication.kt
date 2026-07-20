package com.elemes.enrollment

import com.elemes.common.TenantDataSourceConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableKafka
@EnableScheduling
@Import(TenantDataSourceConfig::class)
class AssignmentEnrollmentApplication

fun main(args: Array<String>) {
    runApplication<AssignmentEnrollmentApplication>(*args)
}
