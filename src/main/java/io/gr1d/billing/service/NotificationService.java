package io.gr1d.billing.service;

import feign.FeignException;
import io.gr1d.billing.api.notification.EmailRequest;
import io.gr1d.billing.api.notification.NotificationApi;
import io.gr1d.billing.model.Notification;
import io.gr1d.billing.repository.NotificationRepository;
import io.gr1d.billing.response.CardResponse;
import io.gr1d.billing.response.InvoiceResponse;
import io.gr1d.billing.response.TenantResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
public class NotificationService {

    private final MessageSource i18n;
    private final Locale locale = new Locale("pt", "BR");
    private final NotificationApi notificationApi;
    private final NotificationRepository notificationRepository;
    private final int maxTries;

    @Autowired
    public NotificationService(final MessageSource i18n, final NotificationApi notificationApi,
                               final NotificationRepository notificationRepository,
                               @Value("${gr1d.notification.maxTries}") final int maxTries) {
        this.i18n = i18n;
        this.notificationApi = notificationApi;
        this.notificationRepository = notificationRepository;
        this.maxTries = maxTries;
    }

    public void invoicePaymentSuccess(final InvoiceResponse invoiceResponse) {

        final EmailRequest emailRequest = createEmailRequest(invoiceResponse, invoiceResponse.getTenant(), null);
        emailRequest.setSubject(i18n("io.gr1d.billing.emailSubject.invoicePaid", "Payment Received"));

        sendEmail("billing-status-paid", emailRequest);
    }

    public void invoicePaymentFailed(final InvoiceResponse invoiceResponse, final boolean isRetry) {
        final EmailRequest emailRequest = createEmailRequest(invoiceResponse, invoiceResponse.getTenant(), null);
        emailRequest.setSubject(i18n("io.gr1d.billing.emailSubject.paymentFailed", "Problem Processing Your Payment"));

        // Send mail to the customer
        if (!isRetry) {
            sendEmail("billing-status-failed", emailRequest);
        } else {
            sendEmail("billing-charge-retry", emailRequest);
        }

        // Send mail to ourselves (gr1d IC staff)
        emailRequest.setTo(invoiceResponse.getTenant().getEmail());
        emailRequest.setSubject(i18n("io.gr1d.billing.emailSubject.internal.statusFailed", "[BILLING ERROR] Payment Error"));
        sendEmail("billing-status-failed-internal", emailRequest);
    }

    public void invoiceFailedCommunication(final InvoiceResponse invoiceResponse) {
        // Send mail to ourselves (gr1d IC staff)
        final EmailRequest emailRequest = createEmailRequest(invoiceResponse, invoiceResponse.getTenant(), null);
        emailRequest.setSubject(i18n("gr1d.portal.billing.email.templates.webhook.statusFailedInternal", "[BILLING ERROR] Payment Error"));

        sendEmail("billing-status-failed-internal", emailRequest);
    }

    public void invoiceRefunding(final InvoiceResponse invoiceResponse) {
        final EmailRequest emailRequest = createEmailRequest(invoiceResponse, invoiceResponse.getTenant(), null);
        emailRequest.setSubject(i18n("io.gr1d.billing.emailSubject.internal.statusRefunding", "[BILLING ERROR] Customer asked for Refund"));

        sendEmail("billing-status-refunding", emailRequest);
    }

    public void invoiceRefunded(final InvoiceResponse invoiceResponse) {
        final EmailRequest emailRequest = createEmailRequest(invoiceResponse, invoiceResponse.getTenant(), null);
        emailRequest.setSubject(i18n("io.gr1d.billing.emailSubject.statusRefunded", "Your Refund is Available"));

        // Send mail to the customer
        sendEmail("billing-status-refunded-internal", emailRequest);

        // Send mail to ourselves (gr1d IC staff)
        emailRequest.setTo(invoiceResponse.getTenant().getEmail());
        emailRequest.setSubject(i18n("io.gr1d.billing.emailSubject.internal.statusRefunded", "[BILLING ERROR] Customer Refunded"));
        sendEmail("billing-status-refunded-internal", emailRequest);
    }

    public void invoiceNoCard(final InvoiceResponse invoiceResponse) {
        final EmailRequest emailRequest = createEmailRequest(invoiceResponse, invoiceResponse.getTenant(), null);
        emailRequest.setSubject(i18n("email.invoiceStatus.failed.subject.customer", "Problem Processing Your Payment. Unable to find card"));

        // Send mail to the customer
        sendEmail("billing-no-card", emailRequest);

        // Send mail to ourselves (gr1d IC staff)
        emailRequest.setTo(invoiceResponse.getTenant().getEmail());
        emailRequest.setSubject(i18n("gr1d.portal.billing.email.templates.webhook.noCard", "[BILLING ERROR] Unable to find card"));
        sendEmail("billing-no-card", emailRequest);
    }

    public void userBlocked(final InvoiceResponse invoiceResponse) {
        final EmailRequest emailRequest = createEmailRequest(invoiceResponse, invoiceResponse.getTenant(), null);
        emailRequest.setSubject(i18n("io.gr1d.billing.emailSubject.accountBlocked", "Your account has been blocked"));

        // Send mail to the customer
        sendEmail("billing-user-blocked", emailRequest);

        // Send mail to ourselves (gr1d IC staff)
        emailRequest.setTo(invoiceResponse.getTenant().getEmail());
        emailRequest.setSubject(i18n("gr1d.portal.billing.email.templates.webhook.userBlocked", "[BILLING INFO] User blocked"));
        sendEmail("billing-user-blocked", emailRequest);
    }

    public void userUnblocked(final InvoiceResponse invoiceResponse) {
        final EmailRequest emailRequest = createEmailRequest(invoiceResponse, invoiceResponse.getTenant(), null);
        emailRequest.setSubject(i18n("io.gr1d.billing.emailSubject.accountUnblocked", "Your account has been unblocked"));

        // Send mail to the customer
        sendEmail("billing-user-unblocked", emailRequest);

        // Send mail to ourselves (gr1d IC staff)
        emailRequest.setTo(invoiceResponse.getTenant().getEmail());
        emailRequest.setSubject(i18n("gr1d.portal.billing.email.templates.webhook.userUnblocked", "[BILLING INFO] User unblocked"));
        sendEmail("billing-user-unblocked", emailRequest);
    }


    public void invoiceCreated(final InvoiceResponse invoiceResponse, final Optional<byte[]> invoicePdf) {
        final EmailRequest emailRequest = createEmailRequest(invoiceResponse, invoiceResponse.getTenant(), null);
        emailRequest.setSubject(i18n("io.gr1d.billing.emailSubject.invoiceCreated", "[gr1d] [Billing] Developer Portal Invoice"));

        invoicePdf.ifPresent(bytes ->
                emailRequest.addAttachment("invoice", bytes, "application/pdf", i18n("io.gr1d.billing.invoice.fileName", "invoice.pdf")));

        sendEmail("billing-create-customer", emailRequest);

    }


    public void invoiceCanceled(final InvoiceResponse invoiceResponse) {

        final EmailRequest emailRequest = createEmailRequest(invoiceResponse, invoiceResponse.getTenant(), null);
        emailRequest.setSubject(i18n("io.gr1d.billing.emailSubject.invoiceCanceled", "[gr1d] [Billing] Developer Portal Invoice"));

        sendEmail("billing-invoice-canceled", emailRequest);
    }

    public void cardAuthorized(final TenantResponse tenant, final CardResponse card) {

        final EmailRequest emailRequest = createEmailRequest(null, tenant, card);
        emailRequest.setSubject(i18n("email.cardAuthorization.subject.customer", "[gr1d] [Billing] Developer Portal Invoice"));

        sendEmail("billing-card-authorization-customer", emailRequest);
    }

    private EmailRequest createEmailRequest(final InvoiceResponse invoiceResponse, final TenantResponse tenant, final CardResponse card) {
        final EmailRequest emailRequest = new EmailRequest();

        emailRequest.setTo(card != null ? card.getUser().getEmail() : invoiceResponse.getUser().getEmail());
        emailRequest.setLocale("pt-br");
        emailRequest.setTenantRealm(tenant.getRealm());
        emailRequest.add("invoice", invoiceResponse);

        if (card != null) {
            emailRequest.add("card", card);
            emailRequest.add("customer", card.getUser());
        }

        return emailRequest;
    }

    private String i18n(final String key, final String defaultValue) {
        return i18n.getMessage(key, null, defaultValue, locale);
    }

    private void sendEmail(final String templateName, final EmailRequest emailRequest) {
        try {
            emailRequest.setTemplate(templateName);
            notificationApi.sendEmail(emailRequest);
        } catch (FeignException e) {
            notificationRepository.save(new Notification(templateName, SerializationUtils.serialize(emailRequest)));
            log.error("Error send email: {}", emailRequest, e);
        }
    }

    protected void syncSendEmail() {

        final Iterable<Notification> notifications = notificationRepository.findAll();

        notifications.forEach(notification -> {
            final EmailRequest emailRequest = SerializationUtils.deserialize(notification.getEmail());
            emailRequest.setTemplate(notification.getTemplateName());
            try {
                notificationApi.sendEmail(emailRequest);
                notificationRepository.delete(notification);
            } catch (FeignException e) {
                log.error("Error send email: {}", emailRequest, e);
                if (notification.getTries() == maxTries) {
                    notificationRepository.delete(notification);
                } else {
                    notification.setTries(notification.getTries() + 1);
                    notificationRepository.save(notification);
                }
            }
        });

    }
}
