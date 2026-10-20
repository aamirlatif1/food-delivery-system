package de.aamir.foodordering.order.messaging.listener

import de.aamir.foodordering.order.application.dto.message.PaymentResponse
import de.aamir.foodordering.order.application.dto.message.RestaurantApprovalResponse
import de.aamir.foodordering.order.application.ports.input.message.listener.PaymentResponseMessageListener
import de.aamir.foodordering.order.application.ports.input.message.listener.RestaurantApprovalResponseMessageListener
import io.micronaut.configuration.kafka.ConsumerRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import kotlin.test.Test
import kotlin.test.assertEquals

// Micronaut validates the retry-topic error strategy only when it starts the consumers, so this
// starts them. Nothing listens on the bootstrap port: the consumers subscribe but never connect.
class KafkaListenerWiringTest {

    @Test
    fun `listeners consume their topic and its retry topic`() {
        val properties = mapOf<String, Any>(
            "spec.name" to SPEC_NAME,
            "kafka.enabled" to "true",
            "kafka.health.enabled" to "false",
            "kafka-config.bootstrap-servers" to "localhost:1",
        )
        ApplicationContext.run(properties).use { context ->
            val registry = context.getBean(ConsumerRegistry::class.java)

            assertEquals(
                setOf(
                    setOf("payment-response-topic", "payment-response-topic.retry"),
                    setOf("restaurant-approval-response-topic", "restaurant-approval-response-topic.retry"),
                ),
                registry.consumerIds.map { registry.getConsumerSubscription(it) }.toSet(),
            )
        }
    }

    // Stand-ins for the application service, which would need repositories.
    @Singleton
    @Requires(property = "spec.name", value = SPEC_NAME)
    @Replaces(PaymentResponseMessageListener::class)
    class StubPaymentResponseMessageListener : PaymentResponseMessageListener {
        override fun paymentCompleted(response: PaymentResponse) = Unit

        override fun paymentCancelled(response: PaymentResponse) = Unit
    }

    @Singleton
    @Requires(property = "spec.name", value = SPEC_NAME)
    @Replaces(RestaurantApprovalResponseMessageListener::class)
    class StubRestaurantApprovalResponseMessageListener : RestaurantApprovalResponseMessageListener {
        override fun orderApproved(response: RestaurantApprovalResponse) = Unit

        override fun orderRejected(response: RestaurantApprovalResponse) = Unit
    }

    private companion object {
        private const val SPEC_NAME = "KafkaListenerWiringTest"
    }
}
