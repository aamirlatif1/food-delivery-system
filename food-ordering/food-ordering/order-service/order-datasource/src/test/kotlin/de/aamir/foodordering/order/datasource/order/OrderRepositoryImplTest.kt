package de.aamir.foodordering.order.datasource.order

import de.aamir.foodordering.common.domain.valueobject.OrderId
import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.order.datasource.DataAccessFixture
import de.aamir.foodordering.order.datasource.MySqlTest
import de.aamir.foodordering.order.datasource.order.adapter.OrderRepositoryImpl
import de.aamir.foodordering.order.datasource.order.repository.OrderItemJdbcRepository
import de.aamir.foodordering.order.datasource.order.repository.OrderJdbcRepository
import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.valueobject.TrackingId
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@MicronautTest(transactional = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderRepositoryImplTest : MySqlTest {

    @Inject
    lateinit var repository: OrderRepositoryImpl

    @Inject
    lateinit var orders: OrderJdbcRepository

    @Inject
    lateinit var orderItems: OrderItemJdbcRepository

    @BeforeEach
    fun cleanUp() {
        orderItems.deleteAll()
        orders.deleteAll()
    }

    @Test
    fun `a new order is stored with its items and notes`() {
        val order = DataAccessFixture.order()

        repository.save(order)

        assertSameOrder(order, assertNotNull(repository.findById(order.id)))
        assertSameOrder(order, assertNotNull(repository.findByTrackingId(order.trackingId)))
    }

    @Test
    fun `an order without notes is stored with null notes`() {
        val order = DataAccessFixture.order(notes = null)

        repository.save(order)

        assertNull(assertNotNull(repository.findById(order.id)).notes)
    }

    @Test
    fun `saving a known order updates its status and failure messages`() {
        val order = repository.save(DataAccessFixture.order())
        val loaded = assertNotNull(repository.findById(order.id))

        loaded.cancel(listOf("Customer has not enough credit"))
        repository.save(loaded)

        val cancelled = assertNotNull(repository.findById(order.id))
        assertEquals(OrderStatus.CANCELLED, cancelled.status)
        assertEquals(listOf("Customer has not enough credit"), cancelled.failureMessages)
        assertEquals(2, cancelled.items.size)
        assertEquals(1, orders.findById(order.id.value).get().version)
    }

    @Test
    fun `unknown ids find nothing`() {
        assertNull(repository.findById(OrderId(UUID.randomUUID())))
        assertNull(repository.findByTrackingId(TrackingId(UUID.randomUUID())))
    }

    private fun assertSameOrder(expected: Order, actual: Order) {
        assertEquals(expected.id, actual.id)
        assertEquals(expected.trackingId, actual.trackingId)
        assertEquals(expected.customerId, actual.customerId)
        assertEquals(expected.restaurantId, actual.restaurantId)
        assertEquals(expected.deliveryAddress, actual.deliveryAddress)
        assertEquals(expected.price, actual.price)
        assertEquals(expected.status, actual.status)
        assertEquals(expected.notes, actual.notes)
        assertEquals(expected.failureMessages, actual.failureMessages)
        assertEquals(
            expected.items.map { listOf(it.id, it.product.id, it.product.name, it.price, it.quantity, it.subTotal) },
            actual.items.map { listOf(it.id, it.product.id, it.product.name, it.price, it.quantity, it.subTotal) },
        )
    }
}
