package de.aamir.foodordering.order

import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@MicronautTest
class OrderServiceTest {

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
