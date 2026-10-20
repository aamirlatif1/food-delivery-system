package de.aamir.foodordering.kafka.config

import io.micronaut.configuration.kafka.config.AbstractKafkaConfiguration
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Singleton
import org.apache.kafka.clients.CommonClientConfigs

// Points the default, producer and consumer configurations (and so the health check) at
// kafka-config.bootstrap-servers, overriding kafka.bootstrap.servers.
@Singleton
class KafkaBootstrapServersApplier(
    private val kafkaConfigData: KafkaConfigData,
) : BeanCreatedEventListener<AbstractKafkaConfiguration<*, *>> {

    override fun onCreated(event: BeanCreatedEvent<AbstractKafkaConfiguration<*, *>>): AbstractKafkaConfiguration<*, *> =
        event.bean.apply {
            config[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = kafkaConfigData.bootstrapServers
        }
}
