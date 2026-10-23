package de.aamir.foodordering.order.datasource.outbox.mapper

import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventPayload
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalOutboxMessage
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentEventPayload
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentOutboxMessage
import de.aamir.foodordering.order.datasource.common.readColumn
import de.aamir.foodordering.order.datasource.outbox.entity.ApprovalOutboxEntity
import de.aamir.foodordering.order.datasource.outbox.entity.PaymentOutboxEntity
import io.micronaut.json.JsonMapper
import jakarta.inject.Singleton

@Singleton
class OutboxDataAccessMapper(private val jsonMapper: JsonMapper) {

    fun paymentMessageToEntity(message: OrderPaymentOutboxMessage) = PaymentOutboxEntity(
        id = message.id,
        sagaId = message.sagaId,
        createdAt = message.createdAt,
        processedAt = message.processedAt,
        type = message.type,
        payload = jsonMapper.writeValueAsString(message.payload),
        orderStatus = message.orderStatus,
        sagaStatus = message.sagaStatus,
        outboxStatus = message.outboxStatus,
        version = message.version,
    )

    fun entityToPaymentMessage(entity: PaymentOutboxEntity) = OrderPaymentOutboxMessage(
        id = entity.id,
        sagaId = entity.sagaId,
        createdAt = entity.createdAt,
        processedAt = entity.processedAt,
        type = entity.type,
        payload = jsonMapper.readColumn(entity.payload, OrderPaymentEventPayload::class.java),
        orderStatus = entity.orderStatus,
        sagaStatus = entity.sagaStatus,
        outboxStatus = entity.outboxStatus,
        version = entity.version,
    )

    fun approvalMessageToEntity(message: OrderApprovalOutboxMessage) = ApprovalOutboxEntity(
        id = message.id,
        sagaId = message.sagaId,
        createdAt = message.createdAt,
        processedAt = message.processedAt,
        type = message.type,
        payload = jsonMapper.writeValueAsString(message.payload),
        orderStatus = message.orderStatus,
        sagaStatus = message.sagaStatus,
        outboxStatus = message.outboxStatus,
        version = message.version,
    )

    fun entityToApprovalMessage(entity: ApprovalOutboxEntity) = OrderApprovalOutboxMessage(
        id = entity.id,
        sagaId = entity.sagaId,
        createdAt = entity.createdAt,
        processedAt = entity.processedAt,
        type = entity.type,
        payload = jsonMapper.readColumn(entity.payload, OrderApprovalEventPayload::class.java),
        orderStatus = entity.orderStatus,
        sagaStatus = entity.sagaStatus,
        outboxStatus = entity.outboxStatus,
        version = entity.version,
    )
}
