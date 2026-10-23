package de.aamir.foodordering.order.ai.config

import io.micronaut.retry.event.CircuitClosedEvent
import io.micronaut.retry.event.CircuitOpenEvent
import io.micronaut.runtime.event.annotation.EventListener
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

// While the circuit is open, every order with notes goes to staff review; these lines say when
// that starts and ends.
@Singleton
class OpenAiCircuitBreakerLogger {

    @EventListener
    fun onOpen(event: CircuitOpenEvent) {
        log.warn(
            "OpenAI circuit opened after {}: order notes go to staff review until it closes",
            event.throwable.javaClass.simpleName,
        )
    }

    @EventListener
    fun onClosed(event: CircuitClosedEvent) {
        log.info("OpenAI circuit closed: order notes are interpreted again")
    }

    private companion object {
        private val log = LoggerFactory.getLogger(OpenAiCircuitBreakerLogger::class.java)
    }
}
