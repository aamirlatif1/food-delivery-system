package de.aamir.foodordering.order.application.saga

interface SagaStep<T> {
    fun process(data: T)

    fun rollback(data: T)
}
