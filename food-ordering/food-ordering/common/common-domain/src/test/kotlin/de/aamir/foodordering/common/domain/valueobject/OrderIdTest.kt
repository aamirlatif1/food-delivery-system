package de.aamir.foodordering.common.domain.valueobject

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class OrderIdTest {

    @Test
    fun `ids with the same UUID are equal`() {
        val uuid = UUID.randomUUID()
        assertEquals(OrderId(uuid), OrderId(uuid))
        assertEquals(OrderId(uuid), OrderId.of(uuid.toString()))
    }

    @Test
    fun `generated ids differ`() {
        assertNotEquals(OrderId.generate(), OrderId.generate())
    }

    @Test
    fun `different id types with the same value are not equal`() {
        class OtherId(value: UUID) : BaseId<UUID>(value)
        val uuid = UUID.randomUUID()
        assertNotEquals<BaseId<UUID>>(OrderId(uuid), OtherId(uuid))
    }
}
