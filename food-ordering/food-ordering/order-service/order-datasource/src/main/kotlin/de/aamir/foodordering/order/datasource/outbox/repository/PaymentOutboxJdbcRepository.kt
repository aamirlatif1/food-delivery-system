package de.aamir.foodordering.order.datasource.outbox.repository

import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.saga.SagaStatus
import de.aamir.foodordering.order.datasource.outbox.entity.PaymentOutboxEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.time.Instant
import java.util.UUID

@JdbcRepository(dialect = Dialect.MYSQL)
interface PaymentOutboxJdbcRepository : CrudRepository<PaymentOutboxEntity, UUID> {

    // SKIP LOCKED: rows another pod is publishing are left out instead of waited for.
    @Query(
        """
        SELECT * FROM payment_outbox
        WHERE type = :type AND outbox_status IN (:outboxStatuses) AND saga_status IN (:sagaStatuses)
        ORDER BY created_at
        FOR UPDATE SKIP LOCKED
        """,
    )
    fun findForPublishing(type: String, outboxStatuses: Collection<String>, sagaStatuses: Collection<String>): List<PaymentOutboxEntity>

    fun findByTypeAndSagaIdAndSagaStatusIn(type: String, sagaId: UUID, sagaStatus: Collection<SagaStatus>): List<PaymentOutboxEntity>

    fun deleteByTypeAndOutboxStatusAndCreatedAtLessThan(type: String, outboxStatus: OutboxStatus, createdAt: Instant): Int
}
