package de.aamir.foodordering.order.application.ports.input.message.listener

import de.aamir.foodordering.order.application.dto.message.RestaurantApprovalResponse

interface RestaurantApprovalResponseMessageListener {
    fun orderApproved(response: RestaurantApprovalResponse)

    fun orderRejected(response: RestaurantApprovalResponse)
}
