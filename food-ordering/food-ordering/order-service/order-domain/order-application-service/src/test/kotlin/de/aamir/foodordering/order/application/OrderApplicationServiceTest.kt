package de.aamir.foodordering.order.application

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.order.application.dto.track.TrackOrderQuery
import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.outbox.model.payment.PaymentOrderStatus
import de.aamir.foodordering.order.application.saga.ORDER_SAGA_NAME
import de.aamir.foodordering.order.application.saga.SagaStatus
import de.aamir.foodordering.order.domain.exception.OrderDomainException
import de.aamir.foodordering.order.domain.exception.OrderNotFoundException
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OrderApplicationServiceTest {

    private val fixture = OrderTestFixture()
    private val service = fixture.orderApplicationService

    @Test
    fun `createOrder saves a PENDING order and a STARTED payment outbox row`() {
        val response = service.createOrder(fixture.createOrderCommand())

        assertEquals(OrderStatus.PENDING, response.orderStatus)
        assertEquals("Order created", response.message)
        val order = fixture.orderRepository.orders.values.single()
        assertEquals(response.orderTrackingId, order.trackingId.value)
        assertEquals("Pizza", order.items.single().product.name)

        val outbox = fixture.paymentOutboxRepository.rows.values.single()
        assertEquals(ORDER_SAGA_NAME, outbox.type)
        assertEquals(SagaStatus.STARTED, outbox.sagaStatus)
        assertEquals(OutboxStatus.STARTED, outbox.outboxStatus)
        assertEquals(OrderStatus.PENDING, outbox.orderStatus)
        assertEquals(order.id.value, outbox.payload.orderId)
        assertEquals(fixture.customerId.value, outbox.payload.customerId)
        assertEquals(BigDecimal("20.00"), outbox.payload.price)
        assertEquals(PaymentOrderStatus.PENDING, outbox.payload.paymentOrderStatus)
        assertEquals(fixture.now, outbox.createdAt)
    }

    @Test
    fun `createOrder rejects an unknown customer or restaurant and saves nothing`() {
        assertFailsWith<OrderDomainException> { service.createOrder(fixture.createOrderCommand(customerId = UUID.randomUUID())) }
        assertFailsWith<OrderDomainException> { service.createOrder(fixture.createOrderCommand(restaurantId = UUID.randomUUID())) }

        assertTrue(fixture.orderRepository.orders.isEmpty())
        assertTrue(fixture.paymentOutboxRepository.rows.isEmpty())
    }

    @Test
    fun `createOrder rejects an unavailable product or a wrong total`() {
        assertFailsWith<OrderDomainException> { service.createOrder(fixture.createOrderCommand(productId = fixture.pasta.id.value)) }
        assertFailsWith<OrderDomainException> { service.createOrder(fixture.createOrderCommand(price = "25.00")) }

        assertTrue(fixture.paymentOutboxRepository.rows.isEmpty())
    }

    @Test
    fun `trackOrder returns status and failure messages`() {
        val created = service.createOrder(fixture.createOrderCommand())

        val tracked = service.trackOrder(TrackOrderQuery(created.orderTrackingId))

        assertEquals(created.orderTrackingId, tracked.orderTrackingId)
        assertEquals(OrderStatus.PENDING, tracked.orderStatus)
        assertTrue(tracked.failureMessages.isEmpty())
    }

    @Test
    fun `trackOrder of an unknown tracking id throws OrderNotFoundException`() {
        assertFailsWith<OrderNotFoundException> { service.trackOrder(TrackOrderQuery(UUID.randomUUID())) }
    }
}
