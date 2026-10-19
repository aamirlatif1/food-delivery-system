package de.aamir.foodordering.order.domain.event

import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.common.domain.valueobject.ProductId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.entity.OrderItem
import de.aamir.foodordering.order.domain.entity.Product
import de.aamir.foodordering.order.domain.valueobject.StreetAddress
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class OrderCreatedEventTest {

    @Test
    fun `carries the created order and when it happened`() {
        val item = OrderItem.builder()
            .product(Product(ProductId.generate(), "Pizza", Money.of("10.00")))
            .quantity(1)
            .price(Money.of("10.00"))
            .subTotal(Money.of("10.00"))
            .build()
        val order = Order.create(
            CustomerId.generate(),
            RestaurantId.generate(),
            StreetAddress("Hauptstr. 1", "10115", "Berlin"),
            Money.of("10.00"),
            listOf(item),
        )
        val occurredAt = Instant.parse("2026-10-19T10:15:30Z")

        val event = OrderCreatedEvent(order, occurredAt)

        assertSame(order, event.aggregate)
        assertEquals(occurredAt, event.occurredAt)
    }
}
