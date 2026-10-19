package de.aamir.foodordering.order.domain.valueobject

import de.aamir.foodordering.common.domain.valueobject.BaseId

// Position of an item within its order, starting at 1.
class OrderItemId(value: Long) : BaseId<Long>(value)
