package de.aamir.foodordering.order.application.outbox.model.approval

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.saga.SagaStatus
import java.time.Instant
import java.util.UUID

// A row of restaurant_approval_outbox. version is the optimistic lock, managed by the repository.
data class OrderApprovalOutboxMessage(
    val id: UUID,
    val sagaId: UUID,
    val createdAt: Instant,
    val processedAt: Instant?,
    val type: String,
    val payload: OrderApprovalEventPayload,
    val orderStatus: OrderStatus,
    val sagaStatus: SagaStatus,
    val outboxStatus: OutboxStatus,
    val version: Int,
)
