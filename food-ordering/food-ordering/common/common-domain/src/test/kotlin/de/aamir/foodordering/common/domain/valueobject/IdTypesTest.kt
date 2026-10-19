package de.aamir.foodordering.common.domain.valueobject

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class IdTypesTest {

    @Test
    fun `product and customer ids compare by UUID`() {
        val uuid = UUID.randomUUID()
        assertEquals(ProductId(uuid), ProductId.of(uuid.toString()))
        assertEquals(CustomerId(uuid), CustomerId.of(uuid.toString()))
        assertNotEquals(ProductId.generate(), ProductId.generate())
        assertNotEquals(CustomerId.generate(), CustomerId.generate())
    }

    @Test
    fun `ids of different types with the same UUID are not equal`() {
        val uuid = UUID.randomUUID()
        assertNotEquals<BaseId<UUID>>(OrderId(uuid), CustomerId(uuid))
        assertNotEquals<BaseId<UUID>>(ProductId(uuid), CustomerId(uuid))
    }
}
