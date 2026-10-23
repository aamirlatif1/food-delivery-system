package de.aamir.foodordering.order.datasource.view.entity

import de.aamir.foodordering.order.datasource.common.BinaryUuid
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

// customer_view: written by the customer-topic listener. version is the customer's version in
// customer-service, so an older event never overwrites a newer one; it is not a lock.
@MappedEntity("customer_view")
data class CustomerViewEntity(
    @Id @BinaryUuid val id: UUID,
    val version: Long,
)
