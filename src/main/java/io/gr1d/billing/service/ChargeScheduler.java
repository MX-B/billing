package io.gr1d.billing.service;

import io.gr1d.billing.repository.InvoiceRepository;
import io.gr1d.core.service.Gr1dClock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
public class ChargeScheduler {

    private final Gr1dClock clock;
    private final ChargeService chargeService;
    private final InvoiceRepository invoiceRepository;

    private final int maxTries;

    @Autowired
    public ChargeScheduler(final Gr1dClock clock, final ChargeService chargeService,
                           final InvoiceRepository invoiceRepository,
                           @Value("${gr1d.charge.maxTries}") final int maxTries) {
        this.clock = clock;
        this.chargeService = chargeService;
        this.invoiceRepository = invoiceRepository;
        this.maxTries = maxTries;
    }

    @Transactional
    @Scheduled(cron = "${gr1d.charge.cron}")
    public void chargeScheduledInvoices() {
        log.info("Starting schedule job to charge invoices");
        final LocalDate localDate = LocalDate.now(clock);
        this.chargeScheduledInvoices(localDate);
    }

    private void chargeScheduledInvoices(final LocalDate localDate) {
        invoiceRepository.findInvoicesToCharge(localDate, maxTries)
                .forEach(invoice -> {
                    try {
                        chargeService.charge(invoice);
                    } catch (final Exception e) {
                        log.error("Error while trying to charge invoice", e);
                    }
                });
    }

}
