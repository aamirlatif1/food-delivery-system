package de.aamir.foodordering.order

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

// REQUIREMENTS §8: dependency direction of the hexagonal layout.
class ArchitectureTest {

    private val adapterPackages = listOf(
        "de.aamir.foodordering.order.api",
        "de.aamir.foodordering.order.messaging",
        "de.aamir.foodordering.order.datasource",
        "de.aamir.foodordering.order.ai",
    )

    private fun String.isAdapter() = adapterPackages.any { startsWith(it) }

    @Test
    fun `common domain depends on neither Micronaut nor jakarta`() {
        Konsist.scopeFromModule("common/common-domain")
            .files
            .assertFalse { file ->
                file.imports.any { it.name.startsWith("io.micronaut.") || it.name.startsWith("jakarta.") }
            }
    }

    @Test
    fun `domain core depends on neither Micronaut, jakarta nor adapters`() {
        Konsist.scopeFromModule("order-service/order-domain/core-domain")
            .files
            .assertFalse { file ->
                file.imports.any {
                    it.name.startsWith("io.micronaut.") ||
                        it.name.startsWith("jakarta.") ||
                        it.name.isAdapter()
                }
            }
    }

    @Test
    fun `application service does not depend on adapters`() {
        Konsist.scopeFromModule("order-service/order-domain/order-application-service")
            .files
            .assertFalse { file -> file.imports.any { it.name.isAdapter() } }
    }

    @Test
    fun `only the AI adapter talks to OpenAI`() {
        Konsist.scopeFromProduction()
            .files
            .filterNot { it.path.contains("/order-service/order-ai/") }
            .assertFalse { file -> file.imports.any { it.name.startsWith("com.openai.") } }
    }
}
