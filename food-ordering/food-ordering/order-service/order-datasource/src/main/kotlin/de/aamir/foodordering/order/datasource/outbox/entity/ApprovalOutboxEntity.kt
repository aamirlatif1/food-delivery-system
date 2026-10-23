package de.aamir.foodordering.order.datasource.outbox.entity

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.saga.SagaStatus
import de.aamir.foodordering.order.datasource.common.BinaryUuid
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Version
import java.time.Instant
import java.util.UUID

@MappedEntity("restaurant_approval_outbox")
data class ApprovalOutboxEntity(
    @Id @BinaryUuid val id: UUID,
    @BinaryUuid val sagaId: UUID,
    val createdAt: Instant,
    val processedAt: Instant?,
    val type: String,
    // JSON: OrderApprovalEventPayload
    val payload: String,
    val orderStatus: OrderStatus,
    val sagaStatus: SagaStatus,
    val outboxStatus: OutboxStatus,
    @Version val version: Int,
)
