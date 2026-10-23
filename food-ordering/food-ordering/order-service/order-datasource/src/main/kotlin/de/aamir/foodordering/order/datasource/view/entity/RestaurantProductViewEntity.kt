package de.aamir.foodordering.order.datasource.view.entity

import de.aamir.foodordering.order.datasource.common.BinaryUuid
import io.micronaut.data.annotation.Embeddable
import io.micronaut.data.annotation.EmbeddedId
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import java.math.BigDecimal
import java.util.UUID

// restaurant_product_view: one row per product of a restaurant, written by the restaurant-topic
// listener. version is the restaurant's version in restaurant-service, not a lock.
@MappedEntity("restaurant_product_view")
data class RestaurantProductViewEntity(
    @EmbeddedId val id: RestaurantProductViewId,
    val name: String,
    val price: BigDecimal,
    val available: Boolean,
    val restaurantActive: Boolean,
    val version: Long,
)

@Embeddable
data class RestaurantProductViewId(
    @MappedProperty("restaurant_id") @BinaryUuid val restaurantId: UUID,
    @MappedProperty("product_id") @BinaryUuid val productId: UUID,
)
