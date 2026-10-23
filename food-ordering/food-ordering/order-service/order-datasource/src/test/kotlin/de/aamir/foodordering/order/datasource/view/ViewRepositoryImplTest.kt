package de.aamir.foodordering.order.datasource.view

import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.common.domain.valueobject.ProductId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.datasource.DataAccessFixture
import de.aamir.foodordering.order.datasource.MySqlTest
import de.aamir.foodordering.order.datasource.view.adapter.CustomerRepositoryImpl
import de.aamir.foodordering.order.datasource.view.adapter.RestaurantRepositoryImpl
import de.aamir.foodordering.order.datasource.view.entity.CustomerViewEntity
import de.aamir.foodordering.order.datasource.view.entity.RestaurantProductViewEntity
import de.aamir.foodordering.order.datasource.view.entity.RestaurantProductViewId
import de.aamir.foodordering.order.datasource.view.repository.CustomerViewJdbcRepository
import de.aamir.foodordering.order.datasource.view.repository.RestaurantProductViewJdbcRepository
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@MicronautTest(transactional = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ViewRepositoryImplTest : MySqlTest {

    @Inject
    lateinit var customerRepository: CustomerRepositoryImpl

    @Inject
    lateinit var restaurantRepository: RestaurantRepositoryImpl

    @Inject
    lateinit var customerView: CustomerViewJdbcRepository

    @Inject
    lateinit var productView: RestaurantProductViewJdbcRepository

    private val restaurantId = RestaurantId(DataAccessFixture.restaurantId)

    @BeforeEach
    fun cleanUp() {
        customerView.deleteAll()
        productView.deleteAll()
    }

    @Test
    fun `a customer in the view is found`() {
        customerView.save(CustomerViewEntity(DataAccessFixture.customerId, version = 1))

        val customer = assertNotNull(customerRepository.findCustomer(CustomerId(DataAccessFixture.customerId)))

        assertEquals(CustomerId(DataAccessFixture.customerId), customer.id)
    }

    @Test
    fun `a customer missing from the view is not found`() {
        assertNull(customerRepository.findCustomer(CustomerId(UUID.randomUUID())))
    }

    @Test
    fun `a restaurant is built from its product rows`() {
        productView.saveAll(
            listOf(
                product(DataAccessFixture.pizzaId, "Pizza", "12.50", available = true, active = true),
                product(DataAccessFixture.saladId, "Salad", "7.25", available = false, active = true),
                // Another restaurant's product is not part of the menu.
                productRow(UUID.randomUUID(), UUID.randomUUID(), "Soup", "4.00", available = true, active = true),
            ),
        )

        val restaurant = assertNotNull(restaurantRepository.findRestaurantInformation(restaurantId))

        assertTrue(restaurant.active)
        assertEquals(2, restaurant.products.size)
        val salad = assertNotNull(restaurant.findProduct(ProductId(DataAccessFixture.saladId)))
        assertEquals("Salad", salad.name)
        assertEquals(Money.of("7.25"), salad.price)
        assertFalse(salad.available)
    }

    @Test
    fun `a restaurant is inactive if any row says so`() {
        productView.saveAll(
            listOf(
                product(DataAccessFixture.pizzaId, "Pizza", "12.50", available = true, active = true),
                product(DataAccessFixture.saladId, "Salad", "7.25", available = true, active = false),
            ),
        )

        assertFalse(assertNotNull(restaurantRepository.findRestaurantInformation(restaurantId)).active)
    }

    @Test
    fun `a restaurant without products in the view is not found`() {
        assertNull(restaurantRepository.findRestaurantInformation(RestaurantId(UUID.randomUUID())))
    }

    private fun product(productId: UUID, name: String, price: String, available: Boolean, active: Boolean) =
        productRow(DataAccessFixture.restaurantId, productId, name, price, available, active)

    private fun productRow(restaurantId: UUID, productId: UUID, name: String, price: String, available: Boolean, active: Boolean) =
        RestaurantProductViewEntity(
            id = RestaurantProductViewId(restaurantId, productId),
            name = name,
            price = BigDecimal(price),
            available = available,
            restaurantActive = active,
            version = 1,
        )
}
