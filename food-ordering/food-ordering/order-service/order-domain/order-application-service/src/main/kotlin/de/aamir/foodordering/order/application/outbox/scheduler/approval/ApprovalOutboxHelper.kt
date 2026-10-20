package de.aamir.foodordering.order.application.outbox.scheduler.approval

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalOutboxMessage
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventPayload
import de.aamir.foodordering.order.application.ports.output.repository.ApprovalOutboxRepository
import de.aamir.foodordering.order.application.saga.ORDER_SAGA_NAME
import de.aamir.foodordering.order.application.saga.SagaStatus
import jakarta.inject.Singleton
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Singleton
class ApprovalOutboxHelper(
    private val repository: ApprovalOutboxRepository,
    private val clock: Clock,
) {

    fun findBySagaIdAndSagaStatus(sagaId: UUID, vararg sagaStatuses: SagaStatus): OrderApprovalOutboxMessage? =
        repository.findByTypeAndSagaIdAndSagaStatus(ORDER_SAGA_NAME, sagaId, sagaStatuses.toList())

    // Approval requests go out once the order is paid (PROCESSING).
    fun findUnpublished(): List<OrderApprovalOutboxMessage> =
        repository.findByTypeAndOutboxStatusAndSagaStatus(
            ORDER_SAGA_NAME,
            OutboxStatus.STARTED,
            listOf(SagaStatus.PROCESSING),
        )

    fun saveNew(payload: OrderApprovalEventPayload, orderStatus: OrderStatus, sagaStatus: SagaStatus, sagaId: UUID): OrderApprovalOutboxMessage =
        repository.save(
            OrderApprovalOutboxMessage(
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

    fun updateSagaStatus(message: OrderApprovalOutboxMessage, orderStatus: OrderStatus, sagaStatus: SagaStatus): OrderApprovalOutboxMessage =
        repository.save(message.copy(orderStatus = orderStatus, sagaStatus = sagaStatus, processedAt = clock.instant()))

    fun updateOutboxStatus(message: OrderApprovalOutboxMessage, outboxStatus: OutboxStatus): OrderApprovalOutboxMessage =
        repository.save(message.copy(outboxStatus = outboxStatus))

    fun deleteCompletedBefore(createdBefore: Instant): Int =
        repository.deleteByTypeAndOutboxStatusAndCreatedAtBefore(ORDER_SAGA_NAME, OutboxStatus.COMPLETED, createdBefore)
}
