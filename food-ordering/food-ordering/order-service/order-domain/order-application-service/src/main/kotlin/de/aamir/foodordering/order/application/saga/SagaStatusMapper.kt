package de.aamir.foodordering.order.application.saga

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.common.domain.valueobject.PaymentStatus

// The one place where order status maps to saga status (REQUIREMENTS §4.3).
object SagaStatusMapper {

    fun fromOrderStatus(orderStatus: OrderStatus): SagaStatus = when (orderStatus) {
        OrderStatus.PENDING -> SagaStatus.STARTED
        OrderStatus.PAID -> SagaStatus.PROCESSING
        OrderStatus.APPROVED -> SagaStatus.SUCCEEDED
        OrderStatus.CANCELING -> SagaStatus.COMPENSATING
        OrderStatus.CANCELLED -> SagaStatus.COMPENSATED
    }

    // A failed payment cancels an order with nothing to undo, so the saga FAILED;
    // a cancelled (refunded) payment ends a compensation.
    fun afterPaymentRollback(paymentStatus: PaymentStatus, orderStatus: OrderStatus): SagaStatus =
        if (paymentStatus == PaymentStatus.FAILED) SagaStatus.FAILED else fromOrderStatus(orderStatus)

    // Saga status a payment response is expected to find on its payment outbox row.
    fun expectedForPaymentResponse(paymentStatus: PaymentStatus): SagaStatus = when (paymentStatus) {
        PaymentStatus.COMPLETED, PaymentStatus.FAILED -> SagaStatus.STARTED
        PaymentStatus.CANCELLED -> SagaStatus.COMPENSATING
    }
}
