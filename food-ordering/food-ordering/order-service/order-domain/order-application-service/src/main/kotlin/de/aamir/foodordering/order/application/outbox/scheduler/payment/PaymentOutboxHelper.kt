package de.aamir.foodordering.order.application.outbox.scheduler.payment

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.outbox.UNPUBLISHED
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentOutboxMessage
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentEventPayload
import de.aamir.foodordering.order.application.ports.output.repository.PaymentOutboxRepository
import de.aamir.foodordering.order.application.saga.ORDER_SAGA_NAME
import de.aamir.foodordering.order.application.saga.SagaStatus
import jakarta.inject.Singleton
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Singleton
class PaymentOutboxHelper(
    private val repository: PaymentOutboxRepository,
    private val clock: Clock,
) {

    fun findBySagaIdAndSagaStatus(sagaId: UUID, vararg sagaStatuses: SagaStatus): OrderPaymentOutboxMessage? =
        repository.findByTypeAndSagaIdAndSagaStatus(ORDER_SAGA_NAME, sagaId, sagaStatuses.toList())

    // Payment requests go out for a new saga (STARTED) and for a refund (COMPENSATING). Rows whose
    // last send failed are sent again.
    fun findUnpublished(): List<OrderPaymentOutboxMessage> =
        repository.findByTypeAndOutboxStatusAndSagaStatus(
            ORDER_SAGA_NAME,
            UNPUBLISHED,
            listOf(SagaStatus.STARTED, SagaStatus.COMPENSATING),
        )

    fun saveNew(payload: OrderPaymentEventPayload, orderStatus: OrderStatus, sagaStatus: SagaStatus, sagaId: UUID): OrderPaymentOutboxMessage =
        repository.save(
            OrderPaymentOutboxMessage(
                id = UUID.randomUUID(),
                sagaId = sagaId,
                createdAt = clock.instant(),
                processedAt = null,
                type = ORDER_SAGA_NAME,
                payload = payload,
                orderStatus = orderStatus,
                sagaStatus = sagaStatus,
                outboxStatus = OutboxStatus.STARTED,
                version = 0,
            ),
        )

    fun updateSagaStatus(message: OrderPaymentOutboxMessage, orderStatus: OrderStatus, sagaStatus: SagaStatus): OrderPaymentOutboxMessage =
        repository.save(message.copy(orderStatus = orderStatus, sagaStatus = sagaStatus, processedAt = clock.instant()))

    fun updateOutboxStatus(message: OrderPaymentOutboxMessage, outboxStatus: OutboxStatus): OrderPaymentOutboxMessage =
        repository.save(message.copy(outboxStatus = outboxStatus))

    fun deleteCompletedBefore(createdBefore: Instant): Int =
        repository.deleteByTypeAndOutboxStatusAndCreatedAtBefore(ORDER_SAGA_NAME, OutboxStatus.COMPLETED, createdBefore)
}
