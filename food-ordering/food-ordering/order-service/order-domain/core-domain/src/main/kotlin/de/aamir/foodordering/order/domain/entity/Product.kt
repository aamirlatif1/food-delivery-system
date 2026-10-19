package de.aamir.foodordering.order.domain.entity

import de.aamir.foodordering.common.domain.entity.BaseEntity
import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.common.domain.valueobject.ProductId

// A restaurant's product as the order service sees it, taken from the restaurant view.
class Product(
    id: ProductId,
    val name: String,
    val price: Money,
) : BaseEntity<ProductId>(id)
