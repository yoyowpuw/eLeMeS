package com.elemes.orghierarchy.infrastructure

import com.elemes.common.JdbcOutboxStore
import com.elemes.common.OutboxPoller
import com.elemes.common.SiloRoutingClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate

@Configuration
class OrgHierarchyConfig {

    @Bean
    fun outboxStore(jdbcTemplate: JdbcTemplate): JdbcOutboxStore =
        JdbcOutboxStore(jdbcTemplate, outboxTable = "outbox")

    @Bean
    fun outboxPoller(outboxStore: JdbcOutboxStore, kafkaTemplate: KafkaTemplate<String, String>, siloRoutingClient: SiloRoutingClient): OutboxPoller =
        OutboxPoller(outboxStore, kafkaTemplate, siloRoutingClient)
}
