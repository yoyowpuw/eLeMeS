package com.elemes.assessment.infrastructure

import com.elemes.common.EventStore
import com.elemes.common.GenericJdbcEventStore
import com.elemes.common.JdbcOutboxStore
import com.elemes.common.OutboxPoller
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate

@Configuration
class AssessmentConfig {

    @Bean
    fun eventStore(jdbcTemplate: JdbcTemplate): EventStore =
        GenericJdbcEventStore(jdbcTemplate, eventsTable = "assessment_events")

    @Bean
    fun outboxStore(jdbcTemplate: JdbcTemplate): JdbcOutboxStore =
        JdbcOutboxStore(jdbcTemplate, outboxTable = "outbox")

    @Bean
    fun outboxPoller(outboxStore: JdbcOutboxStore, kafkaTemplate: KafkaTemplate<String, String>): OutboxPoller =
        OutboxPoller(outboxStore, kafkaTemplate)
}
