package de.aamir.foodordering.order.application.outbox.scheduler.approval

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration

// Deletes COMPLETED rows older than 24h (REQUIREMENTS §6.3).
@Singleton
@Requires(property = "order-service.outbox.scheduler.enabled", notEquals = "false")
// open: Micronaut proxies @Transactional beans, and KSP does not see the all-open plugin.
open class ApprovalOutboxCleanerScheduler(
    private val outboxHelper: ApprovalOutboxHelper,
    private val clock: Clock,
) {

    @Transactional
    @Scheduled(
        fixedDelay = "\${order-service.outbox.cleaner.fixed-delay:1h}",
        initialDelay = "\${order-service.outbox.cleaner.initial-delay:1m}",
    )
    open fun cleanOutboxMessages() {
        val deleted = outboxHelper.deleteCompletedBefore(clock.instant().minus(RETENTION))
        if (deleted > 0) {
            log.info("Deleted {} completed restaurant approval outbox messages", deleted)
        }
    }

    private companion object {
        private val RETENTION: Duration = Duration.ofHours(24)
        private val log = LoggerFactory.getLogger(ApprovalOutboxCleanerScheduler::class.java)
    }
}
