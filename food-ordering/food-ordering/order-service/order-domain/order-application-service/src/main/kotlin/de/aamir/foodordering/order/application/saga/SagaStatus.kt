package de.aamir.foodordering.order.application.saga

enum class SagaStatus {
    STARTED,
    PROCESSING,
    SUCCEEDED,
    COMPENSATING,
    COMPENSATED,
    FAILED,
}
