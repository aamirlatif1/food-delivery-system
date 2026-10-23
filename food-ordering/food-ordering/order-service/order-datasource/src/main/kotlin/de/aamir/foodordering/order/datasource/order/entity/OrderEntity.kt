package de.aamir.foodordering.order.datasource.order.entity

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.order.datasource.common.BinaryUuid
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Version
import java.math.BigDecimal
import java.util.UUID

@MappedEntity("orders")
data class OrderEntity(
    @Id @BinaryUuid val id: UUID,
    @BinaryUuid val trackingId: UUID,
    @BinaryUuid val customerId: UUID,
    @BinaryUuid val restaurantId: UUID,
    val price: BigDecimal,
    val orderStatus: OrderStatus,
    val street: String,
    val postalCode: String,
    val city: String,
    // JSON: OrderNotesColumn, null when the customer wrote no notes.
    val notes: String?,
    // JSON array of strings.
    val failureMessages: String,
    @Version val version: Int,
)
