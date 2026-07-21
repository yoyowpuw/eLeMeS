package com.elemes.orghierarchy

import com.elemes.common.SiloProvisioningConfig
import com.elemes.common.TenantDataSourceConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@Import(TenantDataSourceConfig::class, SiloProvisioningConfig::class)
class OrgHierarchyApplication

fun main(args: Array<String>) {
    runApplication<OrgHierarchyApplication>(*args)
}
