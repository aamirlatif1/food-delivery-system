package de.aamir.foodordering.order

import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.ports.output.repository.PaymentOutboxRepository
import de.aamir.foodordering.order.application.saga.ORDER_SAGA_NAME
import de.aamir.foodordering.order.application.saga.SagaStatus
import de.aamir.foodordering.order.datasource.MySqlTest
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// The REST API through the application service into MySQL: the order and its payment_outbox row
// are written in one transaction (REQUIREMENTS §4.1 step 1).
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderPersistenceTest : MySqlTest {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Inject
    lateinit var paymentOutbox: PaymentOutboxRepository

    private val customerId = UUID.randomUUID()
    private val restaurantId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    // The views are written by Kafka listeners in production; here plain JDBC writes them, outside
    // the application's connection management. UUID_TO_BIN without its swap flag is the byte order
    // the adapter's UuidBinaryConverter uses.
    @BeforeAll
    fun seedViews() {
        val db = properties
        DriverManager.getConnection(
            db.getValue("datasources.default.url"),
            db.getValue("datasources.default.username"),
            db.getValue("datasources.default.password"),
        ).use { connection ->
            connection.prepareStatement("INSERT INTO customer_view (id, version) VALUES (UUID_TO_BIN(?), 1)").use {
                it.setString(1, customerId.toString())
                it.executeUpdate()
            }
            connection.prepareStatement(
                """
                INSERT INTO restaurant_product_view (restaurant_id, product_id, name, price, available, restaurant_active, version)
                VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), 'Pizza', 25.00, TRUE, TRUE, 1)
                """,
            ).use {
                it.setString(1, restaurantId.toString())
                it.setString(2, productId.toString())
                it.executeUpdate()
            }
        }
    }

    @Test
    fun `a created order is stored with its payment outbox row and can be tracked`() {
        val created = client.toBlocking().exchange(HttpRequest.POST("/orders", orderRequest(customerId)), Map::class.java)
        assertEquals(HttpStatus.ACCEPTED, created.status)
        val trackingId = created.body()!!["orderTrackingId"] as String

        val tracked = client.toBlocking().retrieve(HttpRequest.GET<Any>("/orders/$trackingId"), Map::class.java)

        assertEquals("PENDING", tracked["orderStatus"])
        val outboxRows = paymentOutbox.findByTypeAndOutboxStatusAndSagaStatus(
            ORDER_SAGA_NAME,
            OutboxStatus.STARTED,
            listOf(SagaStatus.STARTED),
        ).filter { it.payload.customerId == customerId }
        assertEquals(BigDecimal("50.00"), outboxRows.single().payload.price)
    }

    @Test
    fun `an order for a customer missing from the view is rejected with 422`() {
        val error = assertFailsWith<HttpClientResponseException> {
            client.toBlocking().exchange(HttpRequest.POST("/orders", orderRequest(UUID.randomUUID())), Map::class.java)
        }

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.status)
    }

    private fun orderRequest(customerId: UUID) = mapOf(
        "customerId" to customerId,
        "restaurantId" to restaurantId,
        "address" to mapOf("street" to "Main Street 1", "postalCode" to "10115", "city" to "Berlin"),
        "price" to BigDecimal("50.00"),
        "items" to listOf(
            mapOf("productId" to productId, "quantity" to 2, "price" to BigDecimal("25.00"), "subTotal" to BigDecimal("50.00")),
        ),
    )
}
