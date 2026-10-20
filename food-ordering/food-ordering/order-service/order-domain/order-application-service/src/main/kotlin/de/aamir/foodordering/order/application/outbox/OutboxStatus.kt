package de.aamir.foodordering.order.application.outbox

enum class OutboxStatus {
    STARTED,
    COMPLETED,
    FAILED,
}
