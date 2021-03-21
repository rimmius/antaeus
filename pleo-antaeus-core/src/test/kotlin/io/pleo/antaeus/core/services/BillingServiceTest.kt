package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.external.PaymentProvider
import org.junit.jupiter.api.Test
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import java.math.BigDecimal

class BillingServiceTest {
    val defaultInvoice = Invoice(id = 1,
                                 amount = Money(value = BigDecimal(1.0), currency = Currency.EUR),
                                 status = InvoiceStatus.PENDING,
                                 customerId = 1)
    val paidInvoice = defaultInvoice.copy(status = InvoiceStatus.PAID)

    val differentInvoice = defaultInvoice.copy(customerId = 2)
    val failureInvoice = differentInvoice.copy(status = InvoiceStatus.FAILED)

    val paymentProvider = mockk<PaymentProvider> {
        every { charge(defaultInvoice) } returns true
        every { charge(differentInvoice) } returns false
    }
    val invoiceService = mockk<InvoiceService> {
        every { update(paidInvoice) } returns paidInvoice
        every { update(failureInvoice) } returns failureInvoice
        every { fetchAllWithStatus(InvoiceStatus.PENDING) } returns listOf(defaultInvoice)
        every { fetchAllWithStatus(InvoiceStatus.FAILED) } returns listOf(failureInvoice)
    }
    val jobScheduler = mockk<JobScheduler> {
        every { schedule(any(), any(), any()) } returns true
    }

    val billingService = BillingService(paymentProvider = paymentProvider,
                                        invoiceService = invoiceService,
                                        scheduler = jobScheduler)

    @Test
    fun `init starts two jobschedulers` () {
        billingService.init()
        verify(exactly = 1) { jobScheduler.schedule(Jobs.MONTHLY_BILLING, any(), any()) }
        verify(exactly = 1) { jobScheduler.schedule(Jobs.FAILED_BILLING, any(), any()) }
    }

    @Test
    fun `chargeInvoices updates all invoices in list` () {
        billingService.chargeInvoices(listOf(defaultInvoice, differentInvoice))
        verify(exactly = 2) { paymentProvider.charge(any()) }
        verify(exactly = 2) { invoiceService.update(any()) }
    }

    @Test
    fun `chargeInvoices updates invoiceService with invoice status paid when charge is success` () {
        billingService.chargeInvoices(listOf(defaultInvoice))
        verify(exactly = 1) { invoiceService.update(any()) }
        verify { invoiceService.update(paidInvoice) }
    }

    @Test
    fun `chargeInvoices updates invoiceService with invoice status failed when charge is not sucessful` () {
        billingService.chargeInvoices(listOf(differentInvoice))
        verify(exactly = 1) { invoiceService.update(any()) }
        verify { invoiceService.update(failureInvoice) }
    }

    @Test
    fun `monthlyBillingJob fetches all invoices with PENDING status and runs charging for them` () {
        billingService.monthlyBillingJob()
        verify(exactly = 1) { invoiceService.fetchAllWithStatus(InvoiceStatus.PENDING) }
        verify(exactly = 0) { invoiceService.fetchAllWithStatus(InvoiceStatus.FAILED) }
        verify(exactly = 0) { invoiceService.fetchAllWithStatus(InvoiceStatus.PAID) }
        verify { paymentProvider.charge(defaultInvoice) }
    }

    @Test
    fun `monthlyBillingJob schedules a new job` () {
        billingService.monthlyBillingJob()
        verify(exactly = 1) { jobScheduler.schedule(Jobs.MONTHLY_BILLING, any(), any()) }
    }

    @Test
    fun `retryJob fetches all invoices with FAILED status and runs charging for them` () {
        val failedInvoiceIsPaid = failureInvoice.copy(status = InvoiceStatus.PAID)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(failureInvoice) } returns true
        }
        val invoiceService = mockk<InvoiceService> {
            every { update(failedInvoiceIsPaid) } returns failedInvoiceIsPaid
            every { fetchAllWithStatus(InvoiceStatus.FAILED) } returns listOf(failureInvoice)
        }

        val billingService = BillingService(paymentProvider = paymentProvider,
                                            invoiceService = invoiceService,
                                            scheduler = jobScheduler)
        billingService.retryJob()
        verify(exactly = 0) { invoiceService.fetchAllWithStatus(InvoiceStatus.PENDING) }
        verify(exactly = 1) { invoiceService.fetchAllWithStatus(InvoiceStatus.FAILED) }
        verify(exactly = 0) { invoiceService.fetchAllWithStatus(InvoiceStatus.PAID) }
        verify { paymentProvider.charge(failureInvoice) }
    }

    @Test
    fun `retryJob schedules a new job` () {
        val failedInvoiceIsPaid = failureInvoice.copy(status = InvoiceStatus.PAID)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(failureInvoice) } returns true
        }
        val invoiceService = mockk<InvoiceService> {
            every { update(failedInvoiceIsPaid) } returns failedInvoiceIsPaid
            every { fetchAllWithStatus(InvoiceStatus.FAILED) } returns listOf(failureInvoice)
        }

        val billingService = BillingService(paymentProvider = paymentProvider,
                                            invoiceService = invoiceService,
                                            scheduler = jobScheduler)

        billingService.retryJob()

        verify(exactly = 1) { jobScheduler.schedule(any(), any(), any()) }
        verify(exactly = 1) { jobScheduler.schedule(Jobs.FAILED_BILLING, any(), any()) }
    }
}
