package com.elemes.certification.infrastructure

import com.elemes.common.EventStore
import com.elemes.common.GenericJdbcEventStore
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@Configuration
class CertificateConfig {

    @Bean
    fun eventStore(jdbcTemplate: JdbcTemplate): EventStore =
        GenericJdbcEventStore(jdbcTemplate, eventsTable = "certificate_events")

    /**
     * A dedicated factory for the "org-unit-events" topic, using plain
     * String deserialization — the default `kafkaListenerContainerFactory`
     * (auto-configured from `spring.kafka.consumer.*`) has its
     * `spring.json.value.default.type` fixed to `EnrollmentEventMessage`
     * for "enrollment-events", so a second topic with a different payload
     * type needs its own factory rather than fighting that default.
     */
    @Bean
    fun orgUnitKafkaListenerContainerFactory(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val consumerFactory = DefaultKafkaConsumerFactory<String, String>(
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to "certification",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            )
        )
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply { this.consumerFactory = consumerFactory }
    }
}
