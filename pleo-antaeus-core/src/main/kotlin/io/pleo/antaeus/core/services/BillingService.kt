/*
 Implements handling of periodically charging of `PENDING` and `FAILED` invoices
 */
package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import java.util.Calendar
import java.util.Date

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val scheduler: JobScheduler
) {
    fun init() {
        scheduleNextMonthly()
        scheduleRetryJob()
    }

    fun getNextMonthlyBillingDate(): Date {
        val current = Calendar.getInstance()
        current.add(Calendar.MONTH, 1)
        current.set(Calendar.DAY_OF_MONTH, 1)
        current.set(Calendar.HOUR_OF_DAY, 0)
        current.set(Calendar.MINUTE, 0)
        current.set(Calendar.SECOND, 0)
        val date: Date = current.getTime()
        return date
    }

    fun scheduleNextMonthly() {
        val nextMonthlyDate = getNextMonthlyBillingDate()
        scheduler.schedule(Jobs.MONTHLY_BILLING, {monthlyBillingJob()}, nextMonthlyDate)
    }

    fun getNextRetryJobDate(): Date {
        val current = Calendar.getInstance()
        current.add(Calendar.DATE, 1)
        current.set(Calendar.HOUR_OF_DAY, 2)
        current.set(Calendar.MINUTE, 0)
        current.set(Calendar.SECOND, 0)
        val date: Date = current.getTime()
        return date
    }

    fun scheduleRetryJob() {
        val nextRetryJobDate = getNextRetryJobDate()
        scheduler.schedule(Jobs.FAILED_BILLING, {retryJob()}, nextRetryJobDate)
    }

    fun chargeInvoices(invoices: List<Invoice>) {
        for (invoice in invoices) {
            if (paymentProvider.charge(invoice)) {
                invoiceService.update(invoice.copy(status = InvoiceStatus.PAID))
            } else {
                invoiceService.update(invoice.copy(status = InvoiceStatus.FAILED))
            }
        }
    }

    fun monthlyBillingJob() {
        var pendingInvoices = invoiceService.fetchAllWithStatus(InvoiceStatus.PENDING)
        chargeInvoices(pendingInvoices)
        scheduleNextMonthly()
    }

    fun retryJob() {
        var failedInvoices = invoiceService.fetchAllWithStatus(InvoiceStatus.FAILED)
        chargeInvoices(failedInvoices)
        scheduleRetryJob()
    }
}
