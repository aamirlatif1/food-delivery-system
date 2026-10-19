package de.aamir.foodordering.order.domain.entity

import de.aamir.foodordering.common.domain.entity.AggregateRoot
import de.aamir.foodordering.common.domain.valueobject.ProductId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId

// Read-only copy of a restaurant-service restaurant and its menu, built from the restaurant
// product view. The order domain validates orders against it but never changes it.
class Restaurant(
    id: RestaurantId,
    val active: Boolean,
    products: List<Product>,
) : AggregateRoot<RestaurantId>(id) {

    val products: List<Product> = products.toList()

    fun findProduct(productId: ProductId): Product? = products.firstOrNull { it.id == productId }
}
