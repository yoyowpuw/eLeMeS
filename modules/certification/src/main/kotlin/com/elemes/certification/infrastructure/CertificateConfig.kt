package com.elemes.certification.infrastructure

import com.elemes.common.EventStore
import com.elemes.common.GenericJdbcEventStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate

@Configuration
class CertificateConfig {

    @Bean
    fun eventStore(jdbcTemplate: JdbcTemplate): EventStore =
        GenericJdbcEventStore(jdbcTemplate, eventsTable = "certificate_events")
}
