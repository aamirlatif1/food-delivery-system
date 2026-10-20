package de.aamir.foodordering.order.api

import de.aamir.foodordering.order.api.model.CreateOrderRequest
import de.aamir.foodordering.order.application.dto.create.CreateOrderResponse
import de.aamir.foodordering.order.application.dto.track.TrackOrderQuery
import de.aamir.foodordering.order.application.dto.track.TrackOrderResponse
import de.aamir.foodordering.order.application.ports.input.service.OrderApplicationService
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Status
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import org.slf4j.LoggerFactory
import java.util.UUID

// REQUIREMENTS.md §7. Blocking calls (database, notes interpreter), so off the event loop.
@Controller("/orders")
@ExecuteOn(TaskExecutors.BLOCKING)
class OrderController(
    private val orderApplicationService: OrderApplicationService,
) {

    // 202: the order is PENDING until the saga has run.
    @Post
    @Status(HttpStatus.ACCEPTED)
    fun createOrder(@Body request: CreateOrderRequest): CreateOrderResponse {
        log.info("Creating order for customer {} at restaurant {}", request.customerId, request.restaurantId)
        val response = orderApplicationService.createOrder(request.toCommand())
        log.info("Order created with tracking id {}", response.orderTrackingId)
        return response
    }

    @Get("/{trackingId}")
    fun trackOrder(trackingId: UUID): TrackOrderResponse =
        orderApplicationService.trackOrder(TrackOrderQuery(trackingId))

    private companion object {
        private val log = LoggerFactory.getLogger(OrderController::class.java)
    }
}
