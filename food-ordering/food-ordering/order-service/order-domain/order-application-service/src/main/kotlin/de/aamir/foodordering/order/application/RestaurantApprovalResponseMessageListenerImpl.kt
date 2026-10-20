package de.aamir.foodordering.order.application

import de.aamir.foodordering.order.application.dto.message.RestaurantApprovalResponse
import de.aamir.foodordering.order.application.ports.input.message.listener.RestaurantApprovalResponseMessageListener
import de.aamir.foodordering.order.application.saga.OrderApprovalSaga
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class RestaurantApprovalResponseMessageListenerImpl(
    private val orderApprovalSaga: OrderApprovalSaga,
) : RestaurantApprovalResponseMessageListener {

    override fun orderApproved(response: RestaurantApprovalResponse) {
        orderApprovalSaga.process(response)
        log.info("Order {} is approved by restaurant {}", response.orderId, response.restaurantId)
    }

    override fun orderRejected(response: RestaurantApprovalResponse) {
        orderApprovalSaga.rollback(response)
        log.info("Order {} is rejected by restaurant {}", response.orderId, response.restaurantId)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(RestaurantApprovalResponseMessageListenerImpl::class.java)
    }
}
