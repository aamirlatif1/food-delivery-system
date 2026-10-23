package de.aamir.foodordering.order.datasource.outbox.adapter

import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalOutboxMessage
import de.aamir.foodordering.order.application.ports.output.repository.ApprovalOutboxRepository
import de.aamir.foodordering.order.application.saga.SagaStatus
import de.aamir.foodordering.order.datasource.outbox.mapper.OutboxDataAccessMapper
import de.aamir.foodordering.order.datasource.outbox.repository.ApprovalOutboxJdbcRepository
import jakarta.inject.Singleton
import java.time.Instant
import java.util.UUID

// restaurant_approval_outbox. An update of a row whose version changed since it was read throws OptimisticLockException;
// a second row for the same (type, sagaId, sagaStatus) violates the unique key.
@Singleton
class ApprovalOutboxRepositoryImpl(
    private val outbox: ApprovalOutboxJdbcRepository,
    private val mapper: OutboxDataAccessMapper,
) : ApprovalOutboxRepository {

    override fun save(message: OrderApprovalOutboxMessage): OrderApprovalOutboxMessage {
        val entity = mapper.approvalMessageToEntity(message)
        val saved = if (outbox.existsById(entity.id)) outbox.update(entity) else outbox.save(entity)
        return message.copy(version = saved.version)
    }

    override fun findByTypeAndOutboxStatusAndSagaStatus(
        type: String,
        outboxStatuses: Collection<OutboxStatus>,
        sagaStatuses: Collection<SagaStatus>,
    ): List<OrderApprovalOutboxMessage> =
        outbox.findForPublishing(type, outboxStatuses.map { it.name }, sagaStatuses.map { it.name })
            .map(mapper::entityToApprovalMessage)

    override fun findByTypeAndSagaIdAndSagaStatus(
        type: String,
        sagaId: UUID,
        sagaStatuses: Collection<SagaStatus>,
    ): OrderApprovalOutboxMessage? =
        outbox.findByTypeAndSagaIdAndSagaStatusIn(type, sagaId, sagaStatuses)
            .firstOrNull()
            ?.let(mapper::entityToApprovalMessage)

    override fun deleteByTypeAndOutboxStatusAndCreatedAtBefore(
        type: String,
        outboxStatus: OutboxStatus,
        createdBefore: Instant,
    ): Int = outbox.deleteByTypeAndOutboxStatusAndCreatedAtLessThan(type, outboxStatus, createdBefore)
}
