package de.aamir.foodordering.order.ai.notes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.sun.net.httpserver.HttpServer
import de.aamir.foodordering.order.ai.config.OpenAiProperties
import de.aamir.foodordering.order.domain.valueobject.SpiceLevel
import de.aamir.foodordering.order.application.exception.OrderNotesInterpretationException
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Runs the real SDK against a local stub of the Chat Completions endpoint.
class OpenAiOrderNotesInterpreterTest {

    private val mapper = ObjectMapper()
    private var status = 200
    private var responseBody = completion(content = OrderNotesResponseParserTest.VALID_ANSWER)
    private var lastRequest: JsonNode? = null

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/v1/chat/completions") { exchange ->
            lastRequest = mapper.readTree(exchange.requestBody)
            val bytes = responseBody.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        start()
    }

    private val properties = OpenAiProperties().apply { model = "gpt-test" }
    private val client = OpenAIOkHttpClient.builder()
        .apiKey("test-key")
        .baseUrl("http://127.0.0.1:${server.address.port}/v1")
        .maxRetries(0)
        .build()
    private val interpreter = OpenAiOrderNotesInterpreter(OpenAiChatCompletions(client), properties)

    @AfterTest
    fun stop() {
        client.close()
        server.stop(0)
    }

    @Test
    fun `sends system prompt, user template and strict schema, and parses the answer`() {
        val interpretation = interpreter.interpret("Mild please, no onions on the pizza, peanut allergy")

        assertEquals(SpiceLevel.MILD, interpretation.spiceLevel)
        assertEquals(listOf("peanuts"), interpretation.allergies)

        val request = checkNotNull(lastRequest)
        assertEquals("gpt-test", request["model"].asText())
        assertEquals(0.0, request["temperature"].asDouble())
        assertEquals(false, request["store"].asBoolean())
        val messages = request["messages"]
        assertEquals("system", messages[0]["role"].asText())
        assertTrue(messages[0]["content"].asText().startsWith("You are the Order Notes Interpreter"))
        assertEquals("user", messages[1]["role"].asText())
        assertTrue(messages[1]["content"].asText().contains("<customer_notes>\nMild please, no onions on the pizza, peanut allergy\n</customer_notes>"))
        val format = request["response_format"]
        assertEquals("json_schema", format["type"].asText())
        assertEquals(OrderNotesSchema.NAME, format["json_schema"]["name"].asText())
        assertTrue(format["json_schema"]["strict"].asBoolean())
        assertEquals(false, format["json_schema"]["schema"]["additionalProperties"].asBoolean())
    }

    @Test
    fun `a refusal is not trusted`() {
        responseBody = completion(content = null, refusal = "I can't help with that")
        assertFailsWith<OrderNotesInterpretationException> { interpreter.interpret("notes") }
    }

    @Test
    fun `a truncated answer is not trusted`() {
        responseBody = completion(content = "{\"allergies\": [", finishReason = "length")
        assertFailsWith<OrderNotesInterpretationException> { interpreter.interpret("notes") }
    }

    @Test
    fun `an API error becomes an interpretation failure`() {
        status = 500
        responseBody = """{"error": {"message": "boom", "type": "server_error"}}"""
        assertFailsWith<OrderNotesInterpretationException> { interpreter.interpret("notes") }
    }

    private fun completion(content: String?, refusal: String? = null, finishReason: String = "stop"): String =
        mapper.writeValueAsString(
            mapOf(
                "id" to "chatcmpl-test",
                "object" to "chat.completion",
                "created" to 1_760_000_000,
                "model" to "gpt-test",
                "choices" to listOf(
                    mapOf(
                        "index" to 0,
                        "finish_reason" to finishReason,
                        "logprobs" to null,
                        "message" to mapOf("role" to "assistant", "content" to content, "refusal" to refusal),
                    ),
                ),
            ),
        )
}
