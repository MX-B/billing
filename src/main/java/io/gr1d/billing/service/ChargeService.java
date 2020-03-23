package io.gr1d.billing.service;

import io.gr1d.billing.exception.ChargeException;
import io.gr1d.billing.exception.InvoiceCannotBeChargedException;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.enumerations.PaymentStatus;
import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.repository.InvoiceRepository;
import io.gr1d.billing.service.payment.PaymentStrategy;
import io.gr1d.core.service.Gr1dClock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
public class ChargeService {

    private final Gr1dClock clock;
    private final PaymentStrategy paymentStrategy;
    private final CardService cardService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceStatusService invoiceStatusService;

    @Autowired
    public ChargeService(final Gr1dClock clock, final PaymentStrategy paymentStrategy,
                         final CardService cardService,
                         final InvoiceRepository invoiceRepository,
                         final InvoiceStatusService invoiceStatusService) {
        this.clock = clock;
        this.paymentStrategy = paymentStrategy;
        this.cardService = cardService;
        this.invoiceRepository = invoiceRepository;
        this.invoiceStatusService = invoiceStatusService;
    }

    @Transactional(noRollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    public void chargeAsync(final Invoice invoice) {
        log.debug("Charging asynchronously");
        try {
            charge(invoice);
        } catch (final ChargeException e) {
            log.error("Error charging Invoice asynchronously", e);
        }
    }

    @Transactional(noRollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public void charge(final Invoice invoice) throws ChargeException {
        log.info("Charging Invoice: {}", invoice);

        if (invoice.getValue().compareTo(BigDecimal.ZERO) == 0) {
            log.info("Invoice with value zero marked as paid: {}", invoice);
            invoice.setPaymentDate(LocalDate.now(clock));
            invoice.setSettlementDate(LocalDate.now(clock));
            invoiceStatusService.updateStatus(invoice, PaymentStatus.SUCCESS);
            invoiceRepository.save(invoice);
            return;
        }

        if (!invoice.getPaymentStatus().isChargeable()) {
            log.error("Invoice cannot be charged. Status: {}", invoice.getPaymentStatus());
            throw new InvoiceCannotBeChargedException();
        }

        final Card card = cardService.getForUser(invoice.getUser()).orElse(null);

        if (card != null) {
            log.info("Using Card ID {}", card.getUuid());

            try {
                final PaymentStatus newStatus = PaymentStatus.CREATED.equals(invoice.getPaymentStatus())
                        ? PaymentStatus.PROCESSING
                        : PaymentStatus.PROCESSING_RETRY;

                final String gatewayTransactionId = paymentStrategy.charge(invoice, card);

                invoice.setLastChargeTime(LocalDateTime.now());
                invoice.setCard(card);
                invoice.setGatewayTransactionId(gatewayTransactionId);
                invoiceStatusService.updateStatus(invoice, newStatus);
                invoiceRepository.save(invoice);
            } catch (final Exception e) {
                log.error("Error while charging Bill", e);
                invoiceStatusService.updateStatus(invoice, PaymentStatus.FAILED_COMMUNICATION);
                throw e;
            }
        } else {
            invoiceStatusService.updateStatus(invoice, PaymentStatus.NO_CARD);

            log.error("User does not have an active CardEnrollment!");
            throw new ChargeException("io.gr1d.billing.cardInfoNotFound");
        }
    }

}
