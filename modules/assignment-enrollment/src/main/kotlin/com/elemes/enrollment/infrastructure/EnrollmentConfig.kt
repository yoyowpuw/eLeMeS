package com.elemes.enrollment.infrastructure

import com.elemes.common.EventStore
import com.elemes.common.GenericJdbcEventStore
import com.elemes.common.JdbcOutboxStore
import com.elemes.common.OutboxPoller
import com.elemes.common.ProcessedMessageStore
import com.elemes.common.SiloRoutingClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate

@Configuration
class EnrollmentConfig {

    @Bean
    fun eventStore(jdbcTemplate: JdbcTemplate): EventStore =
        GenericJdbcEventStore(jdbcTemplate, eventsTable = "enrollment_events")

    @Bean
    fun outboxStore(jdbcTemplate: JdbcTemplate): JdbcOutboxStore =
        JdbcOutboxStore(jdbcTemplate, outboxTable = "outbox")

    @Bean
    fun processedMessageStore(jdbcTemplate: JdbcTemplate): ProcessedMessageStore =
        ProcessedMessageStore(jdbcTemplate, table = "processed_messages")

    @Bean
    fun outboxPoller(outboxStore: JdbcOutboxStore, kafkaTemplate: KafkaTemplate<String, String>, siloRoutingClient: SiloRoutingClient): OutboxPoller =
        OutboxPoller(outboxStore, kafkaTemplate, siloRoutingClient)
}
