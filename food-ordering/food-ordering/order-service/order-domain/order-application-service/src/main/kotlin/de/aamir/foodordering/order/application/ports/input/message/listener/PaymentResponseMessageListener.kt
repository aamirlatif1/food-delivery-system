package de.aamir.foodordering.order.application.ports.input.message.listener

import de.aamir.foodordering.order.application.dto.message.PaymentResponse

interface PaymentResponseMessageListener {
    fun paymentCompleted(response: PaymentResponse)

    // PaymentFailed or PaymentCancelled (refund done).
    fun paymentCancelled(response: PaymentResponse)
}
