package io.pleo.antaeus.core.services

import java.util.Date
import java.util.Timer
import kotlin.concurrent.schedule
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

enum class Jobs {
    FAILED_BILLING,
    MONTHLY_BILLING
}

class JobScheduler() {
    fun schedule(name: Jobs, lmbd: () -> Unit, date: Date): Boolean {
        logger.info() { "scheduling: ${name} at ${date}" }
        Timer().schedule(date) {
            lmbd()
        }
        return true
    }
}
