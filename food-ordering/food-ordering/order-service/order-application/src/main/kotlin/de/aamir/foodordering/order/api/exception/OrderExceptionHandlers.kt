package de.aamir.foodordering.order.api.exception

import de.aamir.foodordering.common.application.exception.ProblemExceptionHandler
import de.aamir.foodordering.order.domain.exception.OrderDomainException
import de.aamir.foodordering.order.domain.exception.OrderNotFoundException
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor
import jakarta.inject.Singleton

// Status codes from REQUIREMENTS.md §7, with the domain message as the Problem detail.
// Malformed or invalid requests get 400 from Micronaut itself.
@Produces
@Singleton
class OrderNotFoundExceptionHandler(responseProcessor: ErrorResponseProcessor<*>) :
    ProblemExceptionHandler<OrderNotFoundException>(responseProcessor, HttpStatus.NOT_FOUND)

// A business rule failed, e.g. the restaurant is inactive or a price does not match the menu.
@Produces
@Singleton
class OrderDomainExceptionHandler(responseProcessor: ErrorResponseProcessor<*>) :
    ProblemExceptionHandler<OrderDomainException>(responseProcessor, HttpStatus.UNPROCESSABLE_ENTITY)
