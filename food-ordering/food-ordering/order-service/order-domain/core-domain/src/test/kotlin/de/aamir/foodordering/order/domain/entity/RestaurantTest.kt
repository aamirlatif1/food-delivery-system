package de.aamir.foodordering.order.domain.entity

import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.common.domain.valueobject.ProductId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class RestaurantTest {

    private val pizza = Product(ProductId.generate(), "Pizza", Money.of("10.00"))
    private val pasta = Product(ProductId.generate(), "Pasta", Money.of("8.50"), available = false)

    @Test
    fun `finds a product on the menu by id`() {
        val restaurant = Restaurant(RestaurantId.generate(), active = true, products = listOf(pizza, pasta))

        assertSame(pasta, restaurant.findProduct(pasta.id))
        assertNull(restaurant.findProduct(ProductId.generate()))
    }

    @Test
    fun `keeps its own copy of the menu`() {
        val menu = mutableListOf(pizza)
        val restaurant = Restaurant(RestaurantId.generate(), active = true, products = menu)
        menu += pasta

        assertEquals(listOf(pizza), restaurant.products)
    }
}
