package de.aamir.foodordering.order.domain

import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.entity.Restaurant
import de.aamir.foodordering.order.domain.event.OrderCancelledEvent
import de.aamir.foodordering.order.domain.event.OrderCreatedEvent
import de.aamir.foodordering.order.domain.event.OrderPaidEvent

// Works across Order and Restaurant, so it is neither's method. Returns events; the
// application layer writes them to the outbox, the domain never publishes.
interface OrderDomainService {

    // Checks the order against the restaurant view, then validates and initializes it (PENDING).
    fun validateAndInitiateOrder(order: Order, restaurant: Restaurant): OrderCreatedEvent

    // PaymentCompleted: PENDING -> PAID. The event asks the restaurant for approval.
    fun payOrder(order: Order): OrderPaidEvent

    // OrderApproved: PAID -> APPROVED. The saga ends here.
    fun approveOrder(order: Order)

    // OrderRejected: PAID -> CANCELING. The event asks the payment service for a refund.
    fun cancelOrderPayment(order: Order, failureMessages: List<String>): OrderCancelledEvent

    // PaymentFailed or PaymentCancelled: -> CANCELLED.
    fun cancelOrder(order: Order, failureMessages: List<String>)
}
