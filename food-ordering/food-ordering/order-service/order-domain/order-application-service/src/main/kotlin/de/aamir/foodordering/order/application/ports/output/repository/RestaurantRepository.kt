package de.aamir.foodordering.order.application.ports.output.repository

import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.domain.entity.Restaurant

// Reads the restaurant product view: the restaurant's active flag and its whole menu.
interface RestaurantRepository {
    fun findRestaurantInformation(restaurantId: RestaurantId): Restaurant?
}
