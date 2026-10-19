package de.aamir.foodordering.common.domain.valueobject

abstract class BaseId<T : Any>(val value: T) {

    override fun equals(other: Any?): Boolean =
        this === other || (other != null && other::class == this::class && (other as BaseId<*>).value == value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "${this::class.simpleName}($value)"
}
