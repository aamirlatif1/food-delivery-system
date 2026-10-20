package de.aamir.foodordering.order.application.outbox.scheduler.payment

import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.ports.output.message.publisher.PaymentRequestMessagePublisher
import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

// Publishes STARTED rows and marks them COMPLETED, or FAILED on a send error (REQUIREMENTS §6.3).
// The rows stay locked (SKIP LOCKED) until the transaction ends, so several pods can run this.
@Singleton
@Requires(property = "order-service.outbox.scheduler.enabled", notEquals = "false")
// open: Micronaut proxies @Transactional beans, and KSP does not see the all-open plugin.
open class PaymentOutboxScheduler(
    private val outboxHelper: PaymentOutboxHelper,
    private val publisher: PaymentRequestMessagePublisher,
) {

    @Transactional
    @Scheduled(
        fixedDelay = "\${order-service.outbox.scheduler.fixed-delay:1s}",
        initialDelay = "\${order-service.outbox.scheduler.initial-delay:10s}",
    )
    open fun processOutboxMessages() {
        val messages = outboxHelper.findUnpublished()
        if (messages.isEmpty()) {
            return
        }
        log.info("Publishing {} payment outbox messages", messages.size)
        messages.forEach { message ->
            val outboxStatus = try {
                publisher.publish(message)
                OutboxStatus.COMPLETED
            } catch (e: Exception) {
                log.error("Could not publish payment outbox message {} for saga {}", message.id, message.sagaId, e)
                OutboxStatus.FAILED
            }
            outboxHelper.updateOutboxStatus(message, outboxStatus)
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(PaymentOutboxScheduler::class.java)
    }
}
