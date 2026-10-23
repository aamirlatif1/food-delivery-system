package de.aamir.foodordering.kafka.consumer

import de.aamir.foodordering.kafka.config.KafkaConsumerConfigData
import io.micronaut.configuration.kafka.config.AbstractKafkaConsumerConfiguration
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Singleton
import org.apache.kafka.clients.consumer.ConsumerConfig

// Applies kafka-consumer-config.* to every Micronaut consumer. Settings under
// kafka.consumers.<group-id>.* are kept, and @KafkaListener attributes are applied after this.
@Singleton
class KafkaConsumerConfigApplier(
    private val consumerConfigData: KafkaConsumerConfigData,
) : BeanCreatedEventListener<AbstractKafkaConsumerConfiguration<*, *>> {

    override fun onCreated(
        event: BeanCreatedEvent<AbstractKafkaConsumerConfiguration<*, *>>,
    ): AbstractKafkaConsumerConfiguration<*, *> =
        event.bean.apply {
            consumerConfigData.toProperties().forEach { (name, value) -> config.putIfAbsent(name, value) }
        }
}

internal fun KafkaConsumerConfigData.toProperties(): Map<String, String> = mapOf(
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to enableAutoCommit.toString(),
    ConsumerConfig.ISOLATION_LEVEL_CONFIG to isolationLevel,
    ConsumerConfig.MAX_POLL_RECORDS_CONFIG to maxPollRecords.toString(),
    ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to maxPollInterval.toMillis().toString(),
    ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to sessionTimeout.toMillis().toString(),
    ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG to heartbeatInterval.toMillis().toString(),
    ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG to maxPartitionFetchBytes.toString(),
    ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG to fetchMaxWait.toMillis().toString(),
    ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG to partitionAssignmentStrategy,
)
