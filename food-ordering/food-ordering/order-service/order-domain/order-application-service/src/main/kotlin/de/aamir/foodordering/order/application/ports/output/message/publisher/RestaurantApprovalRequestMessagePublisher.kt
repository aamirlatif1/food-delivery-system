package de.aamir.foodordering.order.application.ports.output.message.publisher

import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalOutboxMessage

interface RestaurantApprovalRequestMessagePublisher {
    // Returns once the broker has acknowledged the message; throws if it could not be sent.
    fun publish(message: OrderApprovalOutboxMessage)
}
