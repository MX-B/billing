package io.gr1d.billing.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InvoiceSettlementScheduler {

    private final InvoiceService invoiceService;

    @Autowired
    public InvoiceSettlementScheduler(final InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Scheduled(cron = "${gr1d.invoiceSettlement.cron}")
    public void updateInvoiceSettlement() {
        log.info("Starting schedule job to update invoice settlement");
        try {
            invoiceService.invoiceSettlementUpdate();
        } catch (final Exception e) {
            log.error("Error while trying to update invoice settlement", e);
        }
    }

}
