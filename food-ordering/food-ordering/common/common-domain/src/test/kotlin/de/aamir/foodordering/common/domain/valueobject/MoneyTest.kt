package de.aamir.foodordering.common.domain.valueobject

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoneyTest {

    @Test
    fun `equality ignores scale`() {
        assertEquals(Money.of("5"), Money.of("5.00"))
        assertEquals(Money.of("5").hashCode(), Money.of("5.00").hashCode())
    }

    @Test
    fun `rounds half even to two decimals`() {
        assertEquals(Money.of("1.12"), Money.of("1.125"))
        assertEquals(Money.of("1.14"), Money.of("1.135"))
    }

    @Test
    fun arithmetic() {
        assertEquals(Money.of("15.50"), Money.of("10.25") + Money.of("5.25"))
        assertEquals(Money.of("5.00"), Money.of("10.25") - Money.of("5.25"))
        assertEquals(Money.of("30.75"), Money.of("10.25") * 3)
    }

    @Test
    fun comparisons() {
        assertTrue(Money.of("0.01").isGreaterThanZero())
        assertFalse(Money.ZERO.isGreaterThanZero())
        assertFalse(Money.of("-1").isGreaterThanZero())
        assertTrue(Money.of("2").isGreaterThan(Money.of("1.99")))
        assertFalse(Money.of("2").isGreaterThan(Money.of("2.00")))
    }
}
