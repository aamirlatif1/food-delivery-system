package de.aamir.foodordering.order.api

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.order.application.dto.create.CreateOrderCommand
import de.aamir.foodordering.order.application.dto.create.CreateOrderResponse
import de.aamir.foodordering.order.application.dto.notes.InterpretOrderNotesCommand
import de.aamir.foodordering.order.application.dto.notes.OrderNotesInterpretation
import de.aamir.foodordering.order.application.dto.notes.OrderNotesResponse
import de.aamir.foodordering.order.application.dto.track.TrackOrderQuery
import de.aamir.foodordering.order.application.dto.track.TrackOrderResponse
import de.aamir.foodordering.order.application.ports.input.service.OrderApplicationService
import de.aamir.foodordering.order.application.ports.input.service.OrderNotesApplicationService
import de.aamir.foodordering.order.domain.exception.OrderDomainException
import de.aamir.foodordering.order.domain.exception.OrderNotFoundException
import de.aamir.foodordering.order.domain.valueobject.InterpretationSource
import de.aamir.foodordering.order.domain.valueobject.SpiceLevel
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@MicronautTest
@Property(name = "spec.name", value = "OrderControllerTest")
@Property(name = "order-service.outbox.scheduler.enabled", value = "false")
class OrderControllerTest {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun `creates an order and returns 202 with the tracking id`() {
        val response = client.toBlocking().exchange(HttpRequest.POST("/orders", orderJson()), Map::class.java)

        assertEquals(HttpStatus.ACCEPTED, response.status)
        val body = response.body()!!
        assertEquals(TRACKING_ID.toString(), body["orderTrackingId"])
        assertEquals("PENDING", body["orderStatus"])
        assertEquals("Order created", body["message"])
    }

    @Test
    fun `rejects an invalid order with 400 problem details`() {
        val invalid = orderJson().replace("\"quantity\": 2", "\"quantity\": 0").replace("Main Street 1", " ")

        val problem = problemFor(HttpRequest.POST("/orders", invalid), HttpStatus.BAD_REQUEST)

        assertContains(problem, "quantity")
        assertContains(problem, "street")
    }

    @Test
    fun `a broken business rule is 422 problem details`() {
        val order = orderJson().replace(RESTAURANT_ID.toString(), INACTIVE_RESTAURANT_ID.toString())

        val problem = problemFor(HttpRequest.POST("/orders", order), HttpStatus.UNPROCESSABLE_ENTITY)

        assertContains(problem, "Restaurant $INACTIVE_RESTAURANT_ID is currently not active")
    }

    @Test
    fun `tracks an order by its tracking id`() {
        val body = client.toBlocking().retrieve(HttpRequest.GET<Any>("/orders/$TRACKING_ID"), Map::class.java)

        assertEquals("CANCELLED", body["orderStatus"])
        assertEquals(listOf("Product 'Pizza' is not available"), body["failureMessages"])
    }

    @Test
    fun `an unknown tracking id is 404 problem details`() {
        val unknown = UUID.randomUUID()

        val problem = problemFor(HttpRequest.GET<Any>("/orders/$unknown"), HttpStatus.NOT_FOUND)

        assertContains(problem, "Could not find order with tracking id $unknown")
    }

    @Test
    fun `a tracking id that is not a UUID is 400`() {
        problemFor(HttpRequest.GET<Any>("/orders/not-a-uuid"), HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `interprets order notes without placing an order`() {
        val body = client.toBlocking().retrieve(
            HttpRequest.POST("/order-notes/interpretation", """{"notes": "extra hot please"}"""),
            Map::class.java,
        )

        assertEquals("extra hot please", body["text"])
        assertEquals("AI", body["source"])
        assertEquals("HOT", (body["interpretation"] as Map<*, *>)["spiceLevel"])
    }

    @Test
    fun `blank order notes are 400`() {
        problemFor(HttpRequest.POST("/order-notes/interpretation", """{"notes": " "}"""), HttpStatus.BAD_REQUEST)
    }

    // Returns the body after checking the status and the Problem Details media type.
    private fun problemFor(request: HttpRequest<*>, status: HttpStatus): String {
        val e = assertFailsWith<HttpClientResponseException> { client.toBlocking().exchange(request, String::class.java) }
        assertEquals(status, e.status)
        assertEquals("application/problem+json", e.response.contentType.map { it.name }.orElse(null))
        return e.response.getBody(String::class.java).orElse("")
    }

    private fun orderJson() = """
        {
          "customerId": "$CUSTOMER_ID",
          "restaurantId": "$RESTAURANT_ID",
          "address": { "street": "Main Street 1", "postalCode": "10115", "city": "Berlin" },
          "price": 50.00,
          "items": [ { "productId": "${UUID.randomUUID()}", "quantity": 2, "price": 25.00, "subTotal": 50.00 } ],
          "notes": "ring twice"
        }
    """.trimIndent()

    @Singleton
    @Requires(property = "spec.name", value = "OrderControllerTest")
    @Replaces(OrderApplicationService::class)
    // open: the interface has @Valid parameters, so Micronaut proxies it.
    open class FakeOrderApplicationService : OrderApplicationService {
        override fun createOrder(command: CreateOrderCommand): CreateOrderResponse {
            if (command.restaurantId == INACTIVE_RESTAURANT_ID) {
                throw OrderDomainException("Restaurant $INACTIVE_RESTAURANT_ID is currently not active")
            }
            return CreateOrderResponse(TRACKING_ID, OrderStatus.PENDING, "Order created", notes = null)
        }

        override fun trackOrder(query: TrackOrderQuery): TrackOrderResponse {
            if (query.orderTrackingId != TRACKING_ID) {
                throw OrderNotFoundException("Could not find order with tracking id ${query.orderTrackingId}")
            }
            return TrackOrderResponse(TRACKING_ID, OrderStatus.CANCELLED, listOf("Product 'Pizza' is not available"), notes = null)
        }
    }

    @Singleton
    @Requires(property = "spec.name", value = "OrderControllerTest")
    @Replaces(OrderNotesApplicationService::class)
    // open: the interface has @Valid parameters, so Micronaut proxies it.
    open class FakeOrderNotesApplicationService : OrderNotesApplicationService {
        override fun interpretNotes(command: InterpretOrderNotesCommand) = OrderNotesResponse(
            text = command.notes,
            interpretation = OrderNotesInterpretation.EMPTY.copy(spiceLevel = SpiceLevel.HOT),
            source = InterpretationSource.AI,
        )
    }

    private companion object {
        val TRACKING_ID: UUID = UUID.fromString("0192f0c1-0000-7000-8000-0000000000aa")
        val CUSTOMER_ID: UUID = UUID.fromString("0192f0c1-0000-7000-8000-0000000000bb")
        val RESTAURANT_ID: UUID = UUID.fromString("0192f0c1-0000-7000-8000-0000000000cc")
        val INACTIVE_RESTAURANT_ID: UUID = UUID.fromString("0192f0c1-0000-7000-8000-0000000000dd")
    }
}
