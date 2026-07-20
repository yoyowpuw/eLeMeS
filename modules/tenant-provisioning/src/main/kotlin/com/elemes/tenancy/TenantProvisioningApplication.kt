package com.elemes.tenancy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TenantProvisioningApplication

fun main(args: Array<String>) {
    runApplication<TenantProvisioningApplication>(*args)
}
