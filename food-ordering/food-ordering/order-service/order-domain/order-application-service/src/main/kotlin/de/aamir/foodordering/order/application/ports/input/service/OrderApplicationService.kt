package de.aamir.foodordering.order.application.ports.input.service

import de.aamir.foodordering.order.application.dto.create.CreateOrderCommand
import de.aamir.foodordering.order.application.dto.create.CreateOrderResponse
import de.aamir.foodordering.order.application.dto.track.TrackOrderQuery
import de.aamir.foodordering.order.application.dto.track.TrackOrderResponse
import jakarta.validation.Valid

// Invalid input throws jakarta.validation.ConstraintViolationException before any work is done.
interface OrderApplicationService {
    fun createOrder(@Valid command: CreateOrderCommand): CreateOrderResponse

    fun trackOrder(@Valid query: TrackOrderQuery): TrackOrderResponse
}
