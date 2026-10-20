package de.aamir.foodordering.order.application.saga

import de.aamir.foodordering.common.domain.valueobject.OrderApprovalStatus
import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.common.domain.valueobject.PaymentStatus
import de.aamir.foodordering.order.application.OrderTestFixture
import de.aamir.foodordering.order.application.dto.notes.OrderNotesInterpretation
import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.outbox.model.approval.RestaurantOrderStatus
import de.aamir.foodordering.order.application.outbox.model.payment.PaymentOrderStatus
import de.aamir.foodordering.order.domain.valueobject.ItemInstruction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The three end-to-end paths of REQUIREMENTS §4, driven through the saga steps.
class OrderSagaTest {

    private val fixture = OrderTestFixture()

    @Test
    fun `happy path - payment completed then restaurant approved`() {
        val (order, sagaId) = fixture.createOrder()

        fixture.paymentSaga.process(fixture.paymentResponse(order, sagaId, PaymentStatus.COMPLETED))

        assertEquals(OrderStatus.PAID, order.status)
        assertEquals(SagaStatus.PROCESSING, fixture.paymentOutbox(sagaId).single().sagaStatus)
        val approval = fixture.approvalOutboxRepository.rows.values.single()
        assertEquals(sagaId, approval.sagaId)
        assertEquals(SagaStatus.PROCESSING, approval.sagaStatus)
        assertEquals(OutboxStatus.STARTED, approval.outboxStatus)
        assertEquals(RestaurantOrderStatus.PAID, approval.payload.restaurantOrderStatus)
        assertEquals(listOf(fixture.pizza.id.value to 2), approval.payload.products.map { it.id to it.quantity })

        fixture.approvalSaga.process(fixture.approvalResponse(order, sagaId, OrderApprovalStatus.APPROVED))

        assertEquals(OrderStatus.APPROVED, order.status)
        assertEquals(SagaStatus.SUCCEEDED, fixture.approvalOutboxRepository.rows.values.single().sagaStatus)
        assertEquals(SagaStatus.SUCCEEDED, fixture.paymentOutbox(sagaId).single().sagaStatus)
    }

    @Test
    fun `payment failed - order CANCELLED and saga FAILED, nothing to undo`() {
        val (order, sagaId) = fixture.createOrder()

        fixture.paymentSaga.rollback(fixture.paymentResponse(order, sagaId, PaymentStatus.FAILED, listOf("Insufficient credit")))

        assertEquals(OrderStatus.CANCELLED, order.status)
        assertEquals(listOf("Insufficient credit"), order.failureMessages)
        val payment = fixture.paymentOutbox(sagaId).single()
        assertEquals(SagaStatus.FAILED, payment.sagaStatus)
        assertEquals(OrderStatus.CANCELLED, payment.orderStatus)
        assertTrue(fixture.approvalOutboxRepository.rows.isEmpty())
    }

    @Test
    fun `restaurant rejected - CANCELING, refund requested, then CANCELLED and COMPENSATED`() {
        val (order, sagaId) = fixture.createOrder()
        fixture.paymentSaga.process(fixture.paymentResponse(order, sagaId, PaymentStatus.COMPLETED))

        fixture.approvalSaga.rollback(
            fixture.approvalResponse(order, sagaId, OrderApprovalStatus.REJECTED, listOf("Product 'Pizza' is not available")),
        )

        assertEquals(OrderStatus.CANCELING, order.status)
        assertEquals(SagaStatus.COMPENSATING, fixture.approvalOutboxRepository.rows.values.single().sagaStatus)
        val refund = fixture.paymentOutbox(sagaId).single { it.sagaStatus == SagaStatus.COMPENSATING }
        assertEquals(OutboxStatus.STARTED, refund.outboxStatus)
        assertEquals(PaymentOrderStatus.CANCELLED, refund.payload.paymentOrderStatus)

        fixture.paymentSaga.rollback(fixture.paymentResponse(order, sagaId, PaymentStatus.CANCELLED))

        assertEquals(OrderStatus.CANCELLED, order.status)
        assertEquals(listOf("Product 'Pizza' is not available"), order.failureMessages)
        assertEquals(
            setOf(SagaStatus.PROCESSING, SagaStatus.COMPENSATED),
            fixture.paymentOutbox(sagaId).map { it.sagaStatus }.toSet(),
        )
        assertEquals(SagaStatus.COMPENSATED, fixture.approvalOutboxRepository.rows.values.single().sagaStatus)
    }

    @Test
    fun `duplicate responses are skipped`() {
        val (order, sagaId) = fixture.createOrder()
        val paid = fixture.paymentResponse(order, sagaId, PaymentStatus.COMPLETED)
        val approved = fixture.approvalResponse(order, sagaId, OrderApprovalStatus.APPROVED)

        fixture.paymentSaga.process(paid)
        fixture.paymentSaga.process(paid)
        fixture.approvalSaga.process(approved)
        fixture.approvalSaga.process(approved)

        assertEquals(OrderStatus.APPROVED, order.status)
        assertEquals(1, fixture.approvalOutboxRepository.rows.size)
    }

    @Test
    fun `a late payment failure after the order was paid is skipped`() {
        val (order, sagaId) = fixture.createOrder()
        fixture.paymentSaga.process(fixture.paymentResponse(order, sagaId, PaymentStatus.COMPLETED))

        fixture.paymentSaga.rollback(fixture.paymentResponse(order, sagaId, PaymentStatus.FAILED))

        assertEquals(OrderStatus.PAID, order.status)
        assertNull(fixture.paymentOutbox(sagaId).firstOrNull { it.sagaStatus == SagaStatus.FAILED })
    }

    @Test
    fun `the restaurant approval request carries the kitchen's part of the order notes`() {
        fixture.notesInterpreter.answer = OrderNotesInterpretation.EMPTY.copy(
            allergies = listOf("peanuts"),
            itemInstructions = listOf(ItemInstruction("pizza", "no onions")),
            deliveryInstructions = "Ring twice",
        )
        val (order, sagaId) = fixture.createOrder(notes = "No onions, peanut allergy, ring twice")

        fixture.paymentSaga.process(fixture.paymentResponse(order, sagaId, PaymentStatus.COMPLETED))

        val notes = checkNotNull(fixture.approvalOutboxRepository.rows.values.single().payload.notes)
        assertEquals("No onions, peanut allergy, ring twice", notes.text)
        assertEquals(listOf("peanuts"), notes.allergies)
        assertEquals(listOf(ItemInstruction("pizza", "no onions")), notes.itemInstructions)
        assertTrue(notes.requiresStaffReview)
    }
}
