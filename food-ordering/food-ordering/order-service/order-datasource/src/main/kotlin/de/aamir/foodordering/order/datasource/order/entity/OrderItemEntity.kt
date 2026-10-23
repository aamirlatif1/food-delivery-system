package de.aamir.foodordering.order.datasource.order.entity

import de.aamir.foodordering.order.datasource.common.BinaryUuid
import io.micronaut.data.annotation.Embeddable
import io.micronaut.data.annotation.EmbeddedId
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import java.math.BigDecimal
import java.util.UUID

@MappedEntity("order_items")
data class OrderItemEntity(
    @EmbeddedId val id: OrderItemEntityId,
    @BinaryUuid val productId: UUID,
    val productName: String,
    val price: BigDecimal,
    val quantity: Int,
    val subTotal: BigDecimal,
)

// id is the item's position within its order, starting at 1.
@Embeddable
data class OrderItemEntityId(
    @MappedProperty("order_id") @BinaryUuid val orderId: UUID,
    @MappedProperty("id") val id: Long,
)
