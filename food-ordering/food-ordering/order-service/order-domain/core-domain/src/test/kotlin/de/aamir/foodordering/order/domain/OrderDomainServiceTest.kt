package de.aamir.foodordering.order.domain

import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.common.domain.valueobject.ProductId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.entity.OrderItem
import de.aamir.foodordering.order.domain.entity.Product
import de.aamir.foodordering.order.domain.entity.Restaurant
import de.aamir.foodordering.order.domain.exception.OrderDomainException
import de.aamir.foodordering.order.domain.valueobject.StreetAddress
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class OrderDomainServiceTest {

    private val now = Instant.parse("2026-10-19T10:15:30Z")
    private val service = OrderDomainServiceImpl(Clock.fixed(now, ZoneOffset.UTC))

    private val restaurantId = RestaurantId.generate()
    private val pizza = Product(ProductId.generate(), "Pizza", Money.of("10.00"))
    private val pasta = Product(ProductId.generate(), "Pasta", Money.of("8.50"), available = false)
    private val restaurant = Restaurant(restaurantId, active = true, products = listOf(pizza, pasta))

    // The request only knows the product id; name and price come from the menu.
    private fun newOrder(
        productId: ProductId = pizza.id,
        price: String = "20.00",
        forRestaurant: RestaurantId = restaurantId,
    ): Order {
        val item = OrderItem.builder()
            .product(Product(productId, "", Money.of("10.00")))
            .quantity(2)
            .price(Money.of("10.00"))
            .subTotal(Money.of("20.00"))
            .build()
        return Order.builder()
            .customerId(CustomerId.generate())
            .restaurantId(forRestaurant)
            .deliveryAddress(StreetAddress("Hauptstr. 1", "10115", "Berlin"))
            .price(Money.of(price))
            .items(listOf(item))
            .build()
    }

    private fun pendingOrder(): Order = newOrder().also { service.validateAndInitiateOrder(it, restaurant) }

    @Test
    fun `validateAndInitiateOrder starts the order and returns OrderCreatedEvent`() {
        val order = newOrder()

        val event = service.validateAndInitiateOrder(order, restaurant)

        assertSame(order, event.aggregate)
        assertEquals(now, event.occurredAt)
        assertEquals(OrderStatus.PENDING, order.status)
        assertSame(pizza, order.items.single().product)
    }

    @Test
    fun `rejects an inactive restaurant`() {
        val closed = Restaurant(restaurantId, active = false, products = listOf(pizza))
        assertFailsWith<OrderDomainException> { service.validateAndInitiateOrder(newOrder(), closed) }
    }

    @Test
    fun `rejects an order for another restaurant`() {
        assertFailsWith<OrderDomainException> {
            service.validateAndInitiateOrder(newOrder(forRestaurant = RestaurantId.generate()), restaurant)
        }
    }

    @Test
    fun `rejects a product that is not on the menu`() {
        assertFailsWith<OrderDomainException> {
            service.validateAndInitiateOrder(newOrder(productId = ProductId.generate()), restaurant)
        }
    }

    @Test
    fun `rejects a product that is not available`() {
        val error = assertFailsWith<OrderDomainException> {
            service.validateAndInitiateOrder(newOrder(productId = pasta.id), restaurant)
        }
        assertEquals("Product 'Pasta' is not available", error.message)
    }

    @Test
    fun `rejects an item price that differs from the menu price`() {
        val repriced = Restaurant(restaurantId, active = true, products = listOf(Product(pizza.id, "Pizza", Money.of("12.00"))))
        assertFailsWith<OrderDomainException> { service.validateAndInitiateOrder(newOrder(), repriced) }
    }

    @Test
    fun `payOrder moves to PAID and returns OrderPaidEvent`() {
        val order = pendingOrder()

        val event = service.payOrder(order)

        assertEquals(OrderStatus.PAID, order.status)
        assertSame(order, event.aggregate)
        assertEquals(now, event.occurredAt)
    }

    @Test
    fun `approveOrder moves a paid order to APPROVED`() {
        val order = pendingOrder().also { service.payOrder(it) }

        service.approveOrder(order)

        assertEquals(OrderStatus.APPROVED, order.status)
    }

    @Test
    fun `restaurant rejection cancels the payment, then the order`() {
        val order = pendingOrder().also { service.payOrder(it) }

        val event = service.cancelOrderPayment(order, listOf("Product 'Pizza' is not available"))
        assertEquals(OrderStatus.CANCELING, order.status)
        assertSame(order, event.aggregate)

        service.cancelOrder(order, emptyList())
        assertEquals(OrderStatus.CANCELLED, order.status)
        assertEquals(listOf("Product 'Pizza' is not available"), order.failureMessages)
    }

    @Test
    fun `payment failure cancels a pending order`() {
        val order = pendingOrder()

        service.cancelOrder(order, listOf("Insufficient credit"))

        assertEquals(OrderStatus.CANCELLED, order.status)
    }
}
