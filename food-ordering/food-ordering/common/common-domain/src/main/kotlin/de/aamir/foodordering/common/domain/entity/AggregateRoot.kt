package de.aamir.foodordering.common.domain.entity

import de.aamir.foodordering.common.domain.valueobject.BaseId

abstract class AggregateRoot<ID : BaseId<*>>(id: ID? = null) : BaseEntity<ID>(id)
