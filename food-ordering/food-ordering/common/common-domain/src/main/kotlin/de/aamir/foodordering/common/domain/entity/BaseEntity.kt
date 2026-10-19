package de.aamir.foodordering.common.domain.entity

import de.aamir.foodordering.common.domain.valueobject.BaseId

// The id is assigned once: in the constructor, or later when a new entity is initialized.
abstract class BaseEntity<ID : BaseId<*>>(id: ID? = null) {

    private var _id: ID? = id

    var id: ID
        get() = checkNotNull(_id) { "${this::class.simpleName} has no id yet" }
        protected set(value) {
            check(_id == null) { "${this::class.simpleName} already has id $_id" }
            _id = value
        }

    val hasId: Boolean get() = _id != null

    // Entities without an id are only equal to themselves.
    override fun equals(other: Any?): Boolean =
        this === other ||
            (_id != null && other != null && other::class == this::class && (other as BaseEntity<*>)._id == _id)

    override fun hashCode(): Int = _id?.hashCode() ?: System.identityHashCode(this)
}
