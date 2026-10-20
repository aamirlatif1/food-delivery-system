package de.aamir.foodordering.order.api

import de.aamir.foodordering.order.api.model.InterpretOrderNotesRequest
import de.aamir.foodordering.order.application.dto.notes.OrderNotesResponse
import de.aamir.foodordering.order.application.ports.input.service.OrderNotesApplicationService
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

// Previews how order notes would be interpreted, without placing an order.
@Controller("/order-notes")
@ExecuteOn(TaskExecutors.BLOCKING)
class OrderNotesController(
    private val orderNotesApplicationService: OrderNotesApplicationService,
) {

    @Post("/interpretation")
    fun interpret(@Body request: InterpretOrderNotesRequest): OrderNotesResponse =
        orderNotesApplicationService.interpretNotes(request.toCommand())
}
