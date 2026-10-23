package de.aamir.foodordering.order.datasource.outbox.adapter

import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentOutboxMessage
import de.aamir.foodordering.order.application.ports.output.repository.PaymentOutboxRepository
import de.aamir.foodordering.order.application.saga.SagaStatus
import de.aamir.foodordering.order.datasource.outbox.mapper.OutboxDataAccessMapper
import de.aamir.foodordering.order.datasource.outbox.repository.PaymentOutboxJdbcRepository
import jakarta.inject.Singleton
import java.time.Instant
import java.util.UUID

// payment_outbox. An update of a row whose version changed since it was read throws OptimisticLockException;
// a second row for the same (type, sagaId, sagaStatus) violates the unique key.
@Singleton
class PaymentOutboxRepositoryImpl(
    private val outbox: PaymentOutboxJdbcRepository,
    private val mapper: OutboxDataAccessMapper,
) : PaymentOutboxRepository {

    override fun save(message: OrderPaymentOutboxMessage): OrderPaymentOutboxMessage {
        val entity = mapper.paymentMessageToEntity(message)
        val saved = if (outbox.existsById(entity.id)) outbox.update(entity) else outbox.save(entity)
        return message.copy(version = saved.version)
    }

    override fun findByTypeAndOutboxStatusAndSagaStatus(
        type: String,
        outboxStatuses: Collection<OutboxStatus>,
        sagaStatuses: Collection<SagaStatus>,
    ): List<OrderPaymentOutboxMessage> =
        outbox.findForPublishing(type, outboxStatuses.map { it.name }, sagaStatuses.map { it.name })
            .map(mapper::entityToPaymentMessage)

    override fun findByTypeAndSagaIdAndSagaStatus(
        type: String,
        sagaId: UUID,
        sagaStatuses: Collection<SagaStatus>,
    ): OrderPaymentOutboxMessage? =
        outbox.findByTypeAndSagaIdAndSagaStatusIn(type, sagaId, sagaStatuses)
            .firstOrNull()
            ?.let(mapper::entityToPaymentMessage)

    override fun deleteByTypeAndOutboxStatusAndCreatedAtBefore(
        type: String,
        outboxStatus: OutboxStatus,
        createdBefore: Instant,
    ): Int = outbox.deleteByTypeAndOutboxStatusAndCreatedAtLessThan(type, outboxStatus, createdBefore)
}
