package de.aamir.foodordering.order.domain.entity

import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.common.domain.valueobject.ProductId
import de.aamir.foodordering.order.domain.valueobject.OrderItemId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OrderItemTest {

    private val pizza = Product(ProductId.generate(), "Pizza", Money.of("10.00"))

    @Test
    fun `builder sets every field`() {
        val item = OrderItem.builder()
            .id(OrderItemId(1))
            .product(pizza)
            .quantity(2)
            .price(Money.of("10.00"))
            .subTotal(Money.of("20.00"))
            .build()

        assertEquals(OrderItemId(1), item.id)
        assertEquals(pizza, item.product)
        assertEquals(2, item.quantity)
        assertEquals(Money.of("10.00"), item.price)
        assertEquals(Money.of("20.00"), item.subTotal)
        assertTrue(item.isPriceValid())
    }

    @Test
    fun `builder fails when a field is missing`() {
        val error = assertFailsWith<IllegalStateException> {
            OrderItem.builder().id(OrderItemId(1)).product(pizza).quantity(1).price(Money.of("10.00")).build()
        }
        assertEquals("OrderItem subTotal is required", error.message)
    }
}
