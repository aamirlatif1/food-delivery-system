package de.aamir.foodordering.order.application

import de.aamir.foodordering.order.application.dto.message.PaymentResponse
import de.aamir.foodordering.order.application.ports.input.message.listener.PaymentResponseMessageListener
import de.aamir.foodordering.order.application.saga.OrderPaymentSaga
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class PaymentResponseMessageListenerImpl(
    private val orderPaymentSaga: OrderPaymentSaga,
) : PaymentResponseMessageListener {

    override fun paymentCompleted(response: PaymentResponse) {
        orderPaymentSaga.process(response)
        log.info("Payment completed for order {}", response.orderId)
    }

    override fun paymentCancelled(response: PaymentResponse) {
        orderPaymentSaga.rollback(response)
        log.info("Payment {} for order {}", response.paymentStatus, response.orderId)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(PaymentResponseMessageListenerImpl::class.java)
    }
}
