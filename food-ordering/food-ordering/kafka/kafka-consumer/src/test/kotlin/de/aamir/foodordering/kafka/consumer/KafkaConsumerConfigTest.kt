package de.aamir.foodordering.kafka.consumer

import io.micronaut.configuration.kafka.config.AbstractKafkaConsumerConfiguration
import io.micronaut.context.ApplicationContext
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

class KafkaConsumerConfigTest {

    private fun consumerConfig(properties: Map<String, Any> = emptyMap()): Properties =
        ApplicationContext.run(properties + KAFKA_ENABLED).use {
            Properties().apply { putAll(it.getBean(AbstractKafkaConsumerConfiguration::class.java).config) }
        }

    @Test
    fun `applies kafka-config and kafka-consumer-config defaults`() {
        val config = consumerConfig()

        assertEquals("localhost:9092,localhost:9094,localhost:9096", config[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG])
        assertEquals("false", config[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG])
        assertEquals("earliest", config[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG])
        assertEquals("300000", config[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG])
        assertEquals(
            "org.apache.kafka.clients.consumer.CooperativeStickyAssignor",
            config[ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG],
        )
    }

    @Test
    fun `binds overrides and keeps settings made under kafka-consumers`() {
        val config = consumerConfig(
            mapOf(
                "kafka-config.bootstrap-servers" to "kafka-1:19092",
                "kafka-consumer-config.max-poll-records" to "50",
                "kafka.consumers.default.session.timeout.ms" to "10000",
            ),
        )

        assertEquals("kafka-1:19092", config[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG])
        assertEquals("50", config[ConsumerConfig.MAX_POLL_RECORDS_CONFIG])
        assertEquals("10000", config[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG])
    }
}

// Micronaut Kafka creates no clients unless some kafka.* property is set.
private val KAFKA_ENABLED = mapOf<String, Any>("kafka.enabled" to "true")
