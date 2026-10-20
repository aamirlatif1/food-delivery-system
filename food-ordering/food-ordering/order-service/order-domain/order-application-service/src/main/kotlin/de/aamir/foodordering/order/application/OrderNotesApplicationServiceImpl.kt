package de.aamir.foodordering.order.application

import de.aamir.foodordering.order.application.dto.notes.InterpretOrderNotesCommand
import de.aamir.foodordering.order.application.dto.notes.OrderNotesResponse
import de.aamir.foodordering.order.application.mapper.OrderDataMapper
import de.aamir.foodordering.order.application.ports.input.service.OrderNotesApplicationService
import jakarta.inject.Singleton
import jakarta.validation.Valid

@Singleton
// open: Micronaut proxies beans with @Valid parameters, and KSP does not see the all-open plugin.
open class OrderNotesApplicationServiceImpl(
    private val orderNotesHelper: OrderNotesHelper,
) : OrderNotesApplicationService {

    override fun interpretNotes(@Valid command: InterpretOrderNotesCommand): OrderNotesResponse =
        OrderDataMapper.orderNotesToResponse(orderNotesHelper.interpret(command.notes))
}
