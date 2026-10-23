package de.aamir.foodordering.order.datasource.outbox

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.outbox.UNPUBLISHED
import de.aamir.foodordering.order.application.saga.ORDER_SAGA_NAME
import de.aamir.foodordering.order.application.saga.SagaStatus
import de.aamir.foodordering.order.datasource.DataAccessFixture
import de.aamir.foodordering.order.datasource.MySqlTest
import de.aamir.foodordering.order.datasource.outbox.adapter.ApprovalOutboxRepositoryImpl
import de.aamir.foodordering.order.datasource.outbox.adapter.PaymentOutboxRepositoryImpl
import de.aamir.foodordering.order.datasource.outbox.repository.ApprovalOutboxJdbcRepository
import de.aamir.foodordering.order.datasource.outbox.repository.PaymentOutboxJdbcRepository
import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.data.exceptions.OptimisticLockException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.transaction.TransactionOperations
import jakarta.inject.Inject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.sql.Connection
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@MicronautTest(transactional = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OutboxRepositoryImplTest : MySqlTest {

    @Inject
    lateinit var paymentOutbox: PaymentOutboxRepositoryImpl

    @Inject
    lateinit var approvalOutbox: ApprovalOutboxRepositoryImpl

    @Inject
    lateinit var paymentRows: PaymentOutboxJdbcRepository

    @Inject
    lateinit var approvalRows: ApprovalOutboxJdbcRepository

    @Inject
    lateinit var transactions: TransactionOperations<Connection>

    private val unpublishedSagas = listOf(SagaStatus.STARTED, SagaStatus.COMPENSATING)

    @BeforeEach
    fun cleanUp() {
        paymentRows.deleteAll()
        approvalRows.deleteAll()
    }

    @Test
    fun `a new row is stored at version 0 with its payload`() {
        val message = DataAccessFixture.paymentOutboxMessage()

        val saved = paymentOutbox.save(message)

        assertEquals(0, saved.version)
        assertEquals(message, paymentOutbox.findByTypeAndSagaIdAndSagaStatus(ORDER_SAGA_NAME, message.sagaId, listOf(SagaStatus.STARTED)))
    }

    @Test
    fun `an approval row keeps products and notes in its payload`() {
        val message = DataAccessFixture.approvalOutboxMessage()

        approvalOutbox.save(message)

        assertEquals(message, approvalOutbox.findByTypeAndSagaIdAndSagaStatus(ORDER_SAGA_NAME, message.sagaId, listOf(SagaStatus.PROCESSING)))
    }

    @Test
    fun `an update bumps the version`() {
        val saved = paymentOutbox.save(DataAccessFixture.paymentOutboxMessage())
        val processedAt = DataAccessFixture.createdAt.plusSeconds(5)

        val updated = paymentOutbox.save(
            saved.copy(orderStatus = OrderStatus.PAID, sagaStatus = SagaStatus.PROCESSING, processedAt = processedAt),
        )

        assertEquals(1, updated.version)
        val stored = assertNotNull(
            paymentOutbox.findByTypeAndSagaIdAndSagaStatus(ORDER_SAGA_NAME, saved.sagaId, listOf(SagaStatus.PROCESSING)),
        )
        assertEquals(updated, stored)
    }

    // Two pods handle the same response: the second update starts from the version both read.
    @Test
    fun `an update from a stale version fails`() {
        val saved = paymentOutbox.save(DataAccessFixture.paymentOutboxMessage())
        paymentOutbox.save(saved.copy(outboxStatus = OutboxStatus.COMPLETED))

        assertFailsWith<OptimisticLockException> {
            paymentOutbox.save(saved.copy(sagaStatus = SagaStatus.PROCESSING))
        }
    }

    @Test
    fun `a second row for the same saga step fails`() {
        val first = paymentOutbox.save(DataAccessFixture.paymentOutboxMessage())

        assertFailsWith<DataAccessException> {
            paymentOutbox.save(DataAccessFixture.paymentOutboxMessage(sagaId = first.sagaId))
        }
    }

    @Test
    fun `publishing finds started and failed rows of the given saga statuses, oldest first`() {
        val later = paymentOutbox.save(
            DataAccessFixture.paymentOutboxMessage(createdAt = DataAccessFixture.createdAt.plusSeconds(2)),
        )
        val earlier = paymentOutbox.save(
            DataAccessFixture.paymentOutboxMessage(sagaStatus = SagaStatus.COMPENSATING),
        )
        val failed = paymentOutbox.save(
            DataAccessFixture.paymentOutboxMessage(
                outboxStatus = OutboxStatus.FAILED,
                createdAt = DataAccessFixture.createdAt.plusSeconds(1),
            ),
        )
        paymentOutbox.save(DataAccessFixture.paymentOutboxMessage(outboxStatus = OutboxStatus.COMPLETED))
        paymentOutbox.save(DataAccessFixture.paymentOutboxMessage(sagaStatus = SagaStatus.PROCESSING))

        val found = transactions.executeWrite {
            paymentOutbox.findByTypeAndOutboxStatusAndSagaStatus(ORDER_SAGA_NAME, UNPUBLISHED, unpublishedSagas)
        }

        assertEquals(listOf(earlier.id, failed.id, later.id), found.map { it.id })
    }

    // REQUIREMENTS §6.3: several pods can run the publisher because each skips the rows another holds.
    @Test
    fun `rows locked by one publisher are skipped by another`() {
        paymentOutbox.save(DataAccessFixture.paymentOutboxMessage())

        transactions.executeWrite {
            val mine = paymentOutbox.findByTypeAndOutboxStatusAndSagaStatus(ORDER_SAGA_NAME, UNPUBLISHED, unpublishedSagas)
            val otherPod = CompletableFuture.supplyAsync {
                transactions.executeWrite {
                    paymentOutbox.findByTypeAndOutboxStatusAndSagaStatus(ORDER_SAGA_NAME, UNPUBLISHED, unpublishedSagas)
                }
            }.get(10, TimeUnit.SECONDS)

            assertEquals(1, mine.size)
            assertEquals(emptyList(), otherPod)
        }
    }

    @Test
    fun `a saga step is not found under another saga status`() {
        val saved = paymentOutbox.save(DataAccessFixture.paymentOutboxMessage())

        assertNull(paymentOutbox.findByTypeAndSagaIdAndSagaStatus(ORDER_SAGA_NAME, saved.sagaId, listOf(SagaStatus.PROCESSING)))
        assertNull(paymentOutbox.findByTypeAndSagaIdAndSagaStatus(ORDER_SAGA_NAME, UUID.randomUUID(), listOf(SagaStatus.STARTED)))
    }

    @Test
    fun `the cleaner deletes only completed rows created before the cut-off`() {
        val cutOff = DataAccessFixture.createdAt.plus(Duration.ofHours(1))
        paymentOutbox.save(DataAccessFixture.paymentOutboxMessage(outboxStatus = OutboxStatus.COMPLETED))
        val recent = paymentOutbox.save(
            DataAccessFixture.paymentOutboxMessage(outboxStatus = OutboxStatus.COMPLETED, createdAt = cutOff.plusSeconds(1)),
        )
        val started = paymentOutbox.save(DataAccessFixture.paymentOutboxMessage())

        val deleted = paymentOutbox.deleteByTypeAndOutboxStatusAndCreatedAtBefore(ORDER_SAGA_NAME, OutboxStatus.COMPLETED, cutOff)

        assertEquals(1, deleted)
        assertEquals(setOf(recent.id, started.id), paymentRows.findAll().map { it.id }.toSet())
    }
}
