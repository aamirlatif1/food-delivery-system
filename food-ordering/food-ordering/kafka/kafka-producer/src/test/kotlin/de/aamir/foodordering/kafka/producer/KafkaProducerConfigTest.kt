package de.aamir.foodordering.kafka.producer

import io.micronaut.configuration.kafka.config.AbstractKafkaProducerConfiguration
import io.micronaut.context.ApplicationContext
import io.micronaut.core.type.Argument
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// Building the configuration and the Producer does not connect to a broker.
class KafkaProducerConfigTest {

    private fun producerConfig(properties: Map<String, Any> = emptyMap()): Properties =
        ApplicationContext.run(properties + KAFKA_ENABLED).use {
            Properties().apply { putAll(it.getBean(AbstractKafkaProducerConfiguration::class.java).config) }
        }

    @Test
    fun `applies kafka-config and kafka-producer-config defaults`() {
        val config = producerConfig()

        assertEquals("localhost:9092,localhost:9094,localhost:9096", config[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG])
        assertEquals("all", config[ProducerConfig.ACKS_CONFIG])
        assertEquals("true", config[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG])
        assertEquals("120000", config[ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG])
    }

    @Test
    fun `binds overrides and keeps settings made under kafka-producers`() {
        val config = producerConfig(
            mapOf(
                "kafka-config.bootstrap-servers" to "kafka-1:19092",
                "kafka-producer-config.linger" to "20ms",
                "kafka.producers.default.compression.type" to "zstd",
            ),
        )

        assertEquals("kafka-1:19092", config[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG])
        assertEquals("20", config[ProducerConfig.LINGER_MS_CONFIG])
        assertEquals("zstd", config[ProducerConfig.COMPRESSION_TYPE_CONFIG])
    }

    @Test
    fun `provides a String to ByteArray producer`() {
        ApplicationContext.run(KAFKA_ENABLED).use {
            val type = Argument.of(KafkaProducer::class.java, String::class.java, ByteArray::class.java)
            assertNotNull(it.getBean(type))
        }
    }
}

// Micronaut Kafka creates no clients unless some kafka.* property is set.
private val KAFKA_ENABLED = mapOf<String, Any>("kafka.enabled" to "true")
