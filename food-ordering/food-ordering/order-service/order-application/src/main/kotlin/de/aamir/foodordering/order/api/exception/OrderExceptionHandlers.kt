package de.aamir.foodordering.order.api.exception

import de.aamir.foodordering.order.domain.exception.OrderDomainException
import de.aamir.foodordering.order.domain.exception.OrderNotFoundException
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.ExceptionHandler
import io.micronaut.http.server.exceptions.response.ErrorContext
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor
import io.micronaut.problem.HttpStatusType
import jakarta.inject.Singleton
import org.zalando.problem.Problem

// Status codes from REQUIREMENTS.md §7, rendered by micronaut-problem-json as RFC 9457 Problem
// Details. problem-json hides the message of an ordinary exception; these domain messages are
// written for the client, so they are passed on as a Problem whose detail it keeps. Malformed or
// invalid requests get 400 from Micronaut itself.
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

@Produces
@Singleton
class OrderNotFoundExceptionHandler(responseProcessor: ErrorResponseProcessor<*>) :
    ProblemExceptionHandler<OrderNotFoundException>(responseProcessor, HttpStatus.NOT_FOUND)

// A business rule failed, e.g. the restaurant is inactive or a price does not match the menu.
@Produces
@Singleton
class OrderDomainExceptionHandler(responseProcessor: ErrorResponseProcessor<*>) :
    ProblemExceptionHandler<OrderDomainException>(responseProcessor, HttpStatus.UNPROCESSABLE_ENTITY)
