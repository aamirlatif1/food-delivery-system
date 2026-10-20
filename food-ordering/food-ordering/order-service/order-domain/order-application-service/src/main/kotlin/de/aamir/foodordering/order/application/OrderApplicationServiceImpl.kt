package de.aamir.foodordering.order.application

import de.aamir.foodordering.order.application.dto.create.CreateOrderCommand
import de.aamir.foodordering.order.application.dto.create.CreateOrderResponse
import de.aamir.foodordering.order.application.dto.track.TrackOrderQuery
import de.aamir.foodordering.order.application.dto.track.TrackOrderResponse
import de.aamir.foodordering.order.application.mapper.OrderDataMapper
import de.aamir.foodordering.order.application.ports.input.service.OrderApplicationService
import de.aamir.foodordering.order.application.ports.output.repository.OrderRepository
import de.aamir.foodordering.order.domain.exception.OrderNotFoundException
import de.aamir.foodordering.order.domain.valueobject.TrackingId
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import jakarta.validation.Valid

@Singleton
// open: Micronaut proxies @Transactional beans, and KSP does not see the all-open plugin.
open class OrderApplicationServiceImpl(
    private val orderCreateHelper: OrderCreateHelper,
    private val orderNotesHelper: OrderNotesHelper,
    private val orderRepository: OrderRepository,
) : OrderApplicationService {

    // Not transactional: the notes are interpreted first, then OrderCreateHelper saves in one transaction.
    override fun createOrder(@Valid command: CreateOrderCommand): CreateOrderResponse {
        val notes = command.notes?.takeIf { it.isNotBlank() }?.let { orderNotesHelper.interpret(it) }
        val order = orderCreateHelper.persistOrder(command, notes)
        return OrderDataMapper.orderToCreateOrderResponse(order, "Order created")
    }

    @Transactional(readOnly = true)
    override fun trackOrder(@Valid query: TrackOrderQuery): TrackOrderResponse {
        val order = orderRepository.findByTrackingId(TrackingId(query.orderTrackingId))
            ?: throw OrderNotFoundException("Could not find order with tracking id ${query.orderTrackingId}")
        return OrderDataMapper.orderToTrackOrderResponse(order)
    }
}
