package de.aamir.foodordering.common.domain.valueobject

import java.math.BigDecimal
import java.math.RoundingMode

// Amount is always held at scale 2, so 5, 5.0 and 5.00 are the same Money.
class Money(amount: BigDecimal) : Comparable<Money> {

    val amount: BigDecimal = amount.setScale(SCALE, ROUNDING)

    fun isGreaterThanZero(): Boolean = amount.signum() > 0

    fun isGreaterThan(other: Money): Boolean = this > other

    operator fun plus(other: Money): Money = Money(amount + other.amount)

    operator fun minus(other: Money): Money = Money(amount - other.amount)

    operator fun times(multiplier: Int): Money = Money(amount * BigDecimal(multiplier))

    override fun compareTo(other: Money): Int = amount.compareTo(other.amount)

    override fun equals(other: Any?): Boolean = this === other || (other is Money && other.amount == amount)

    override fun hashCode(): Int = amount.hashCode()

    override fun toString(): String = amount.toPlainString()

    companion object {
        private const val SCALE = 2
        private val ROUNDING = RoundingMode.HALF_EVEN

        val ZERO = Money(BigDecimal.ZERO)

        fun of(amount: String): Money = Money(BigDecimal(amount))
    }
}
