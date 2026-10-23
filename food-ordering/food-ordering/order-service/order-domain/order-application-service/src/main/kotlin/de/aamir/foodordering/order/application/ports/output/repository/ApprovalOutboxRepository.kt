package de.aamir.foodordering.order.application.ports.output.repository

import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalOutboxMessage
import de.aamir.foodordering.order.application.saga.SagaStatus
import java.time.Instant
import java.util.UUID

// restaurant_approval_outbox. Unique on (type, sagaId, sagaStatus); save() fails on a stale version.
interface ApprovalOutboxRepository {
    fun save(message: OrderApprovalOutboxMessage): OrderApprovalOutboxMessage

    // Oldest first. Locks the rows it returns (FOR UPDATE SKIP LOCKED) so several pods can publish.
    fun findByTypeAndOutboxStatusAndSagaStatus(
        type: String,
        outboxStatuses: Collection<OutboxStatus>,
        sagaStatuses: Collection<SagaStatus>,
    ): List<OrderApprovalOutboxMessage>

    fun findByTypeAndSagaIdAndSagaStatus(
        type: String,
        sagaId: UUID,
        sagaStatuses: Collection<SagaStatus>,
    ): OrderApprovalOutboxMessage?

    // Returns the number of rows deleted.
    fun deleteByTypeAndOutboxStatusAndCreatedAtBefore(
        type: String,
        outboxStatus: OutboxStatus,
        createdBefore: Instant,
    ): Int
}
