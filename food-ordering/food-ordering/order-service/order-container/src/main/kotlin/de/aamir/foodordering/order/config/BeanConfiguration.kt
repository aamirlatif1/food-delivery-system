package de.aamir.foodordering.order.config

import de.aamir.foodordering.order.domain.OrderDomainService
import de.aamir.foodordering.order.domain.OrderDomainServiceImpl
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import java.time.Clock

// The domain is plain Kotlin, so its services are wired here rather than annotated.
@Factory
class BeanConfiguration {

    @Singleton
    fun clock(): Clock = Clock.systemUTC()

    @Singleton
    fun orderDomainService(clock: Clock): OrderDomainService = OrderDomainServiceImpl(clock)
}
