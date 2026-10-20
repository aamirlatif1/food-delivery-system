package de.aamir.foodordering.order.application.ports.output.repository

import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentOutboxMessage
import de.aamir.foodordering.order.application.saga.SagaStatus
import java.time.Instant
import java.util.UUID

// payment_outbox. Unique on (type, sagaId, sagaStatus); save() fails on a stale version.
interface PaymentOutboxRepository {
    fun save(message: OrderPaymentOutboxMessage): OrderPaymentOutboxMessage

    // Locks the rows it returns (FOR UPDATE SKIP LOCKED) so several pods can publish.
    fun findByTypeAndOutboxStatusAndSagaStatus(
        type: String,
        outboxStatus: OutboxStatus,
        sagaStatuses: Collection<SagaStatus>,
    ): List<OrderPaymentOutboxMessage>

    fun findByTypeAndSagaIdAndSagaStatus(
        type: String,
        sagaId: UUID,
        sagaStatuses: Collection<SagaStatus>,
    ): OrderPaymentOutboxMessage?

    // Returns the number of rows deleted.
    fun deleteByTypeAndOutboxStatusAndCreatedAtBefore(
        type: String,
        outboxStatus: OutboxStatus,
        createdBefore: Instant,
    ): Int
}
