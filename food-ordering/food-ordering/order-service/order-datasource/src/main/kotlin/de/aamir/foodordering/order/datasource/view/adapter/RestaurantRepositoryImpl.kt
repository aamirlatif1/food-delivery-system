package de.aamir.foodordering.order.datasource.view.adapter

import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.common.domain.valueobject.ProductId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.application.ports.output.repository.RestaurantRepository
import de.aamir.foodordering.order.datasource.view.repository.RestaurantProductViewJdbcRepository
import de.aamir.foodordering.order.domain.entity.Product
import de.aamir.foodordering.order.domain.entity.Restaurant
import jakarta.inject.Singleton

@Singleton
class RestaurantRepositoryImpl(private val products: RestaurantProductViewJdbcRepository) : RestaurantRepository {

    // A restaurant is known once the view has at least one of its products. Every row carries the
    // restaurant's active flag; if the rows disagree, the restaurant counts as inactive.
    override fun findRestaurantInformation(restaurantId: RestaurantId): Restaurant? {
        val rows = products.findByIdRestaurantId(restaurantId.value)
        if (rows.isEmpty()) {
            return null
        }
        return Restaurant(
            id = restaurantId,
            active = rows.all { it.restaurantActive },
            products = rows.map { Product(ProductId(it.id.productId), it.name, Money(it.price), it.available) },
        )
    }
}
