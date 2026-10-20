package de.aamir.foodordering.order.application

import de.aamir.foodordering.common.domain.valueobject.OrderId
import de.aamir.foodordering.order.application.ports.output.repository.OrderRepository
import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.exception.OrderNotFoundException
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class OrderSagaHelper(private val orderRepository: OrderRepository) {

    fun findOrder(orderId: UUID): Order =
        orderRepository.findById(OrderId(orderId))
            ?: throw OrderNotFoundException("Order with id $orderId could not be found")

    fun saveOrder(order: Order): Order = orderRepository.save(order)
}
