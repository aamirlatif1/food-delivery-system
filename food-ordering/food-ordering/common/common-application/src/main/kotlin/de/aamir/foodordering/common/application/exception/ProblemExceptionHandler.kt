package de.aamir.foodordering.common.application.exception

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.server.exceptions.ExceptionHandler
import io.micronaut.http.server.exceptions.response.ErrorContext
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor
import io.micronaut.problem.HttpStatusType
import org.zalando.problem.Problem

// Renders an exception as an RFC 9457 Problem Details response via micronaut-problem-json.
// problem-json hides the message of an ordinary exception; subclasses handle exceptions whose
// messages are written for the client, so they are passed on as a Problem whose detail it keeps.
abstract class ProblemExceptionHandler<T : Exception>(
    private val responseProcessor: ErrorResponseProcessor<*>,
    private val status: HttpStatus,
) : ExceptionHandler<T, HttpResponse<*>> {

    override fun handle(request: HttpRequest<*>, exception: T): HttpResponse<*> =
        responseProcessor.processResponse(
            ErrorContext.builder(request).cause(problem(exception)).build(),
            HttpResponse.status<Any>(status),
        )

    private fun problem(exception: T) = Problem.builder()
        .withTitle(status.reason)
        .withStatus(HttpStatusType(status))
        .withDetail(exception.message)
        .build()
}
