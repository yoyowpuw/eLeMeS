package com.elemes.certification

import com.elemes.common.SiloProvisioningConfig
import com.elemes.common.TenantDataSourceConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableKafka
@Import(TenantDataSourceConfig::class, SiloProvisioningConfig::class)
class CertificationApplication

fun main(args: Array<String>) {
    runApplication<CertificationApplication>(*args)
}
