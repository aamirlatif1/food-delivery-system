package de.aamir.foodordering.order

import de.aamir.foodordering.order.datasource.MySqlTest
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Starts against a real MySQL, so Flyway and the repository beans are part of the check.
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderServiceTest : MySqlTest {

    @Inject
    lateinit var application: EmbeddedApplication<*>

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun `application starts`() {
        assertTrue(application.isRunning)
    }

    @Test
    fun `health endpoint reports UP`() {
        val body = client.toBlocking().retrieve(HttpRequest.GET<Any>("/health"), Map::class.java)
        assertEquals("UP", body["status"])
    }
}
