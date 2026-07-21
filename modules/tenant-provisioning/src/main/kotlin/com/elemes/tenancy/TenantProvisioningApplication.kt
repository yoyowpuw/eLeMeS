package com.elemes.tenancy

import com.elemes.common.CorsConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(CorsConfig::class)
class TenantProvisioningApplication

fun main(args: Array<String>) {
    runApplication<TenantProvisioningApplication>(*args)
}
