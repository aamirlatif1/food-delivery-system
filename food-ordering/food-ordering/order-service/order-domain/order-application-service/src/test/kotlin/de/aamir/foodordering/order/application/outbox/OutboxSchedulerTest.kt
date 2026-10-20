package de.aamir.foodordering.order.application.outbox

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.common.domain.valueobject.PaymentStatus
import de.aamir.foodordering.order.application.OrderTestFixture
import de.aamir.foodordering.order.application.RecordingApprovalPublisher
import de.aamir.foodordering.order.application.RecordingPaymentPublisher
import de.aamir.foodordering.order.application.outbox.scheduler.approval.ApprovalOutboxScheduler
import de.aamir.foodordering.order.application.outbox.scheduler.payment.PaymentOutboxCleanerScheduler
import de.aamir.foodordering.order.application.outbox.scheduler.payment.PaymentOutboxScheduler
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutboxSchedulerTest {

    private val fixture = OrderTestFixture()

    @Test
    fun `payment scheduler publishes STARTED rows once and marks them COMPLETED`() {
        val publisher = RecordingPaymentPublisher()
        val scheduler = PaymentOutboxScheduler(fixture.paymentOutboxHelper, publisher)
        val (_, sagaId) = fixture.createOrder()

        scheduler.processOutboxMessages()
        scheduler.processOutboxMessages()

        assertEquals(listOf(sagaId), publisher.published.map { it.sagaId })
        assertEquals(OutboxStatus.COMPLETED, fixture.paymentOutbox(sagaId).single().outboxStatus)
    }

    @Test
    fun `a send error marks the row FAILED`() {
        val scheduler = PaymentOutboxScheduler(fixture.paymentOutboxHelper, RecordingPaymentPublisher(failing = true))
        val (_, sagaId) = fixture.createOrder()

        scheduler.processOutboxMessages()

        assertEquals(OutboxStatus.FAILED, fixture.paymentOutbox(sagaId).single().outboxStatus)
    }

    @Test
    fun `approval scheduler publishes the approval request of a paid order`() {
        val publisher = RecordingApprovalPublisher()
        val (order, sagaId) = fixture.createOrder()
        fixture.paymentSaga.process(fixture.paymentResponse(order, sagaId, PaymentStatus.COMPLETED))

        ApprovalOutboxScheduler(fixture.approvalOutboxHelper, publisher).processOutboxMessages()

        assertEquals(OrderStatus.PAID, publisher.published.single().orderStatus)
        assertEquals(OutboxStatus.COMPLETED, fixture.approvalOutboxRepository.rows.values.single().outboxStatus)
    }

    @Test
    fun `cleaner deletes COMPLETED rows older than 24h only`() {
        val (_, sagaId) = fixture.createOrder()
        PaymentOutboxScheduler(fixture.paymentOutboxHelper, RecordingPaymentPublisher()).processOutboxMessages()

        PaymentOutboxCleanerScheduler(fixture.paymentOutboxHelper, fixture.clock).cleanOutboxMessages()
        assertEquals(1, fixture.paymentOutbox(sagaId).size)

        val dayLater = Clock.fixed(fixture.now.plus(Duration.ofHours(25)), ZoneOffset.UTC)
        PaymentOutboxCleanerScheduler(fixture.paymentOutboxHelper, dayLater).cleanOutboxMessages()
        assertTrue(fixture.paymentOutbox(sagaId).isEmpty())
    }
}
