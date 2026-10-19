package de.aamir.foodordering.order.domain.entity

import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.common.domain.valueobject.OrderId
import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.common.domain.valueobject.ProductId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.domain.exception.OrderDomainException
import de.aamir.foodordering.order.domain.valueobject.OrderItemId
import de.aamir.foodordering.order.domain.valueobject.StreetAddress
import de.aamir.foodordering.order.domain.valueobject.TrackingId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OrderTest {

    private val address = StreetAddress("Hauptstr. 1", "10115", "Berlin")

    private fun item(quantity: Int, price: String, subTotal: String, productPrice: String = price) =
        OrderItem.builder()
            .product(Product(ProductId.generate(), "Pizza", Money.of(productPrice)))
            .quantity(quantity)
            .price(Money.of(price))
            .subTotal(Money.of(subTotal))
            .build()

    private fun order(
        price: String = "25.00",
        items: List<OrderItem> = listOf(item(2, "10.00", "20.00"), item(1, "5.00", "5.00")),
    ) = Order.create(CustomerId.generate(), RestaurantId.generate(), address, Money.of(price), items)

    @Test
    fun `create starts PENDING with tracking id and numbered items`() {
        val order = order()
        assertEquals(OrderStatus.PENDING, order.status)
        assertEquals(listOf(1L, 2L), order.items.map { it.id.value })
        assertTrue(order.failureMessages.isEmpty())
    }

    @Test
    fun `initializeOrder assigns UUID ids, PENDING and item numbers to a new order`() {
        val order = Order.builder()
            .customerId(CustomerId.generate())
            .restaurantId(RestaurantId.generate())
            .deliveryAddress(address)
            .price(Money.of("10.00"))
            .items(listOf(item(1, "10.00", "10.00")))
            .build()
        assertFalse(order.hasId)
        assertFalse(order.items.single().hasId)

        order.initializeOrder()

        assertTrue(order.hasId)
        assertEquals(OrderStatus.PENDING, order.status)
        assertNotEquals(order.id.value, order.trackingId.value)
        assertEquals(OrderItemId(1), order.items.single().id)
    }

    @Test
    fun `initializeOrder twice throws`() {
        assertFailsWith<OrderDomainException> { order().initializeOrder() }
    }

    @Test
    fun `transitions on an uninitialized order throw`() {
        val order = Order.builder()
            .customerId(CustomerId.generate())
            .restaurantId(RestaurantId.generate())
            .deliveryAddress(address)
            .price(Money.of("10.00"))
            .items(listOf(item(1, "10.00", "10.00")))
            .build()
        assertFailsWith<OrderDomainException> { order.pay() }
    }

    @Test
    fun `create rejects invalid prices`() {
        assertFailsWith<OrderDomainException> { order(items = emptyList()) }
        assertFailsWith<OrderDomainException> { order(price = "30.00") }
        assertFailsWith<OrderDomainException> { order(price = "21.00", items = listOf(item(2, "10.00", "21.00"))) }
        assertFailsWith<OrderDomainException> { order(price = "0.00", items = listOf(item(1, "0.00", "0.00"))) }
        assertFailsWith<OrderDomainException> { order(price = "0.00", items = listOf(item(0, "10.00", "0.00"))) }
        assertFailsWith<OrderDomainException> {
            order(price = "10.00", items = listOf(item(1, "10.00", "10.00", productPrice = "12.00")))
        }
    }

    @Test
    fun `happy path PENDING to PAID to APPROVED`() {
        val order = order()
        order.pay()
        assertEquals(OrderStatus.PAID, order.status)
        order.approve()
        assertEquals(OrderStatus.APPROVED, order.status)
    }

    @Test
    fun `payment failed cancels a PENDING order`() {
        val order = order()
        order.cancel(listOf("Insufficient credit"))
        assertEquals(OrderStatus.CANCELLED, order.status)
        assertEquals(listOf("Insufficient credit"), order.failureMessages)
    }

    @Test
    fun `restaurant rejection goes PAID to CANCELING to CANCELLED and keeps messages`() {
        val order = order()
        order.pay()
        order.initCancel(listOf("Product 'Pizza' is not available", " "))
        assertEquals(OrderStatus.CANCELING, order.status)
        order.cancel(emptyList())
        assertEquals(OrderStatus.CANCELLED, order.status)
        assertEquals(listOf("Product 'Pizza' is not available"), order.failureMessages)
    }

    @Test
    fun `every other transition throws`() {
        assertFailsWith<OrderDomainException> { order().approve() }
        assertFailsWith<OrderDomainException> { order().initCancel(emptyList()) }
        assertFailsWith<OrderDomainException> { order().apply { pay() }.pay() }
        assertFailsWith<OrderDomainException> { order().apply { pay() }.cancel(emptyList()) }
        assertFailsWith<OrderDomainException> { order().apply { pay(); approve() }.cancel(emptyList()) }
        assertFailsWith<OrderDomainException> { order().apply { cancel(emptyList()) }.pay() }
    }

    @Test
    fun `blank address is rejected`() {
        assertFailsWith<OrderDomainException> { StreetAddress(" ", "10115", "Berlin") }
    }

    @Test
    fun `builder restores an existing order as given`() {
        val id = OrderId.generate()
        val trackingId = TrackingId.generate()
        val product = Product(ProductId.generate(), "Pizza", Money.of("10.00"))
        val item = OrderItem(OrderItemId(1), product, 1, Money.of("10.00"), Money.of("10.00"))

        val order = Order.builder()
            .id(id)
            .customerId(CustomerId.generate())
            .restaurantId(RestaurantId.generate())
            .deliveryAddress(address)
            .price(Money.of("10.00"))
            .items(listOf(item))
            .trackingId(trackingId)
            .status(OrderStatus.CANCELLED)
            .failureMessages(listOf("Insufficient credit"))
            .build()

        assertEquals(id, order.id)
        assertEquals(trackingId, order.trackingId)
        assertEquals(OrderStatus.CANCELLED, order.status)
        assertEquals(listOf(item), order.items)
        assertEquals(listOf("Insufficient credit"), order.failureMessages)
    }

    @Test
    fun `builder fails when a required field is missing`() {
        val error = assertFailsWith<IllegalStateException> { Order.builder().id(OrderId.generate()).build() }
        assertEquals("Order customerId is required", error.message)
    }
}
