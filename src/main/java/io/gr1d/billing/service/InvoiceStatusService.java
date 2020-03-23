package io.gr1d.billing.service;

import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.enumerations.PaymentStatus;
import io.gr1d.billing.model.enumerations.UserStatus;
import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.model.invoice.InvoicePaymentStatusHistory;
import io.gr1d.billing.repository.InvoiceRepository;
import io.gr1d.billing.repository.InvoiceStatusHistoryRepository;
import io.gr1d.billing.response.InvoiceResponse;
import io.gr1d.core.service.Gr1dClock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
public class InvoiceStatusService {

    private final Gr1dClock clock;
    private final TenantService tenantService;
    private final CardService cardService;
    private final InvoiceStatusHistoryRepository invoiceStatusHistoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final NotificationService notificationService;
    private final UserService userService;
    private final int maxTries;

    @Autowired
    public InvoiceStatusService(final Gr1dClock clock, final TenantService tenantService, 
                                final CardService cardService,
                                final InvoiceStatusHistoryRepository invoiceStatusHistoryRepository,
                                final InvoiceRepository invoiceRepository,
                                final NotificationService notificationService,
                                final UserService userService,
                                @Value("${gr1d.charge.maxTries}") final int maxTries) {
        this.clock = clock;
        this.tenantService = tenantService;
        this.cardService = cardService;
        this.invoiceStatusHistoryRepository = invoiceStatusHistoryRepository;
        this.invoiceRepository = invoiceRepository;
        this.notificationService = notificationService;
        this.userService = userService;
        this.maxTries = maxTries;
    }

    public void updateStatus(final Invoice invoice, final PaymentStatus status) {
        updateStatus(invoice, status, null);
    }

    public void updateStatus(final Invoice invoice, final PaymentStatus status, final String gatewayStatus) {
        final Tenant tenant = tenantService.getTenantDataByRealm(invoice.getUser().getTenantRealm());
        final Card card = cardService.getForUser(invoice.getUser()).orElse(null);
        final InvoiceResponse invoiceResponse = new InvoiceResponse(Locale.getDefault(), card, tenant,
                invoice, null, userService.getKeycloakUser(invoice.getUser()), null, null);

        notifyStatusChange(invoice, status, invoiceResponse);

        if (PaymentStatus.FAILED.equals(status) || PaymentStatus.NO_CARD.equals(status)) {
            // We won't charge after maxTries quantity
            if (invoice.getChargeTries() < maxTries) {
                invoice.setScheduledChargeTime(LocalDate.now(clock).plusDays(5));
            }
            invoice.setChargeTries(invoice.getChargeTries() + 1);

            if (invoice.getChargeTries() >= maxTries && UserStatus.ACTIVE.equals(invoice.getUser().getStatus())) {
                log.info("Bloking User: {}", invoice.getUser().getKeycloakId());
                userService.blockUser(invoice.getUser());
                notificationService.userBlocked(invoiceResponse);
            }
        } else if (PaymentStatus.FAILED_COMMUNICATION.equals(status)) {
            // Something went wrong, scheduling for the next day
            invoice.setScheduledChargeTime(LocalDate.now(clock).plusDays(1));
        } else if (PaymentStatus.SUCCESS.equals(status)) {
            if (UserStatus.BLOCKED.equals(invoice.getUser().getStatus())) {
                log.info("Unblocking User: {}", invoice.getUser().getKeycloakId());
                userService.unblockUser(invoice.getUser());
                notificationService.userUnblocked(invoiceResponse);
            }
            invoice.setPaymentDate(LocalDate.now(clock));
        }

        log.warn("Updated invoice {} to status {}!", invoice.getUuid(), status);

        final InvoicePaymentStatusHistory statusHistory = new InvoicePaymentStatusHistory();
        statusHistory.setGatewayStatus(gatewayStatus);
        statusHistory.setInvoice(invoice);
        statusHistory.setPreviousStatus(invoice.getPaymentStatus());
        statusHistory.setStatus(status);
        statusHistory.setTimestamp(LocalDateTime.now(clock));
        statusHistory.setCard(card);
        invoice.setPaymentStatus(status);
        invoice.setUpdatedAt(LocalDateTime.now());

        invoiceStatusHistoryRepository.save(statusHistory);
        invoiceRepository.save(invoice);
    }

    private void notifyStatusChange(final Invoice invoice, final PaymentStatus status, final InvoiceResponse invoiceResponse) {
        if (PaymentStatus.SUCCESS.equals(status) && invoice.getValue().compareTo(BigDecimal.ZERO) > 0) {
            notificationService.invoicePaymentSuccess(invoiceResponse);
        } else if (PaymentStatus.FAILED.equals(status)) {
            notificationService.invoicePaymentFailed(invoiceResponse, invoice.getChargeTries() > 1);
        } else if (PaymentStatus.REFUNDING.equals(status)) {
            notificationService.invoiceRefunding(invoiceResponse);
        } else if (PaymentStatus.REFUNDED.equals(status)) {
            notificationService.invoiceRefunded(invoiceResponse);
        } else if (PaymentStatus.NO_CARD.equals(status)) {
            notificationService.invoiceNoCard(invoiceResponse);
        } else if (PaymentStatus.FAILED_COMMUNICATION.equals(status)) {
            notificationService.invoiceFailedCommunication(invoiceResponse);
        } else if (PaymentStatus.CANCELED.equals(status)) {
            notificationService.invoiceCanceled(invoiceResponse);
        }
    }

}
