package de.aamir.foodordering.order.ai.notes

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpServer
import de.aamir.foodordering.order.application.exception.OrderNotesInterpretationException
import de.aamir.foodordering.order.application.ports.output.ai.OrderNotesInterpreter
import io.micronaut.context.ApplicationContext
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// The circuit breaker is AOP, so this runs the interpreter from a Micronaut context against a
// local stub of the Chat Completions endpoint that counts the requests reaching it.
class OpenAiCircuitBreakerTest {

    private val mapper = ObjectMapper()
    private val requests = AtomicInteger()

    // Shared with the stub server's handler thread.
    @Volatile
    private var status = 200

    @Volatile
    private var responseBody = completion(content = OrderNotesResponseParserTest.VALID_ANSWER)

    // Requests after this many answer normally; set by failNextRequestsThenAnswer.
    @Volatile
    private var failingRequests = Int.MAX_VALUE

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/v1/chat/completions") { exchange ->
            if (requests.incrementAndGet() > failingRequests) {
                answerNormally()
            }
            exchange.requestBody.readAllBytes()
            val bytes = responseBody.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        start()
    }

    private val context = ApplicationContext.run(
        mapOf(
            "order-service.ai.openai.api-key" to "test-key",
            "order-service.ai.openai.base-url" to "http://127.0.0.1:${server.address.port}/v1",
            "order-service.ai.openai.circuit-breaker.retry-delay" to "10ms",
            "order-service.ai.openai.circuit-breaker.reset" to "${RESET_MILLIS}ms",
        ),
    )
    private val interpreter = context.getBean(OrderNotesInterpreter::class.java)

    @AfterTest
    fun stop() {
        context.close()
        server.stop(0)
    }

    @Test
    fun `a failed call is retried once, then calls fail fast without reaching OpenAI`() {
        failWithServerError()

        assertFailsWith<OrderNotesInterpretationException> { interpreter.interpret("notes") }
        assertEquals(2, requests.get())

        assertFailsWith<OrderNotesInterpretationException> { interpreter.interpret("notes") }
        assertFailsWith<OrderNotesInterpretationException> { interpreter.interpret("notes") }
        assertEquals(2, requests.get())
    }

    @Test
    fun `a call that succeeds on the retry keeps the circuit closed`() {
        failWithServerError()
        failNextRequestsThenAnswer(1)

        interpreter.interpret("notes")
        interpreter.interpret("notes")

        assertEquals(3, requests.get())
    }

    @Test
    fun `after the reset period a successful call closes the circuit`() {
        failWithServerError()
        assertFailsWith<OrderNotesInterpretationException> { interpreter.interpret("notes") }
        answerNormally()

        Thread.sleep(RESET_MILLIS * 3)
        interpreter.interpret("notes")
        interpreter.interpret("notes")

        assertEquals(4, requests.get())
    }

    // OpenAI answered, so it is up: no retry, and the next order still asks OpenAI.
    @Test
    fun `a refusal is neither retried nor opens the circuit`() {
        responseBody = completion(content = null, refusal = "I can't help with that")
        assertFailsWith<OrderNotesInterpretationException> { interpreter.interpret("notes") }
        answerNormally()

        interpreter.interpret("notes")

        assertEquals(2, requests.get())
    }

    private fun failWithServerError() {
        status = 500
        responseBody = """{"error": {"message": "boom", "type": "server_error"}}"""
    }

    private fun failNextRequestsThenAnswer(count: Int) {
        failingRequests = count
    }

    private fun answerNormally() {
        status = 200
        responseBody = completion(content = OrderNotesResponseParserTest.VALID_ANSWER)
    }

    private fun completion(content: String?, refusal: String? = null): String =
        mapper.writeValueAsString(
            mapOf(
                "id" to "chatcmpl-test",
                "object" to "chat.completion",
                "created" to 1_760_000_000,
                "model" to "gpt-test",
                "choices" to listOf(
                    mapOf(
                        "index" to 0,
                        "finish_reason" to "stop",
                        "logprobs" to null,
                        "message" to mapOf("role" to "assistant", "content" to content, "refusal" to refusal),
                    ),
                ),
            ),
        )

    private companion object {
        private const val RESET_MILLIS = 200L
    }
}
