package io.gr1d.billing.response;

import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.spring.keycloak.model.User;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Getter
public class InvoiceResponse implements Serializable {

    private final String uuid;
    private final String number;
    private final CardResponse card;
    private final TenantResponse tenant;
    private final UserResponse user;

    private final String status;
    private final BigDecimal value;
    private final List<InvoiceItemResponse> items;
    private final LocalDate scheduledChargeTime;
    private final LocalDate expirationDate;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final LocalDate paymentDate;
    private final LocalDate settlementDate;

    private final LocalDateTime createdAt;
    private final LocalDateTime lastChargeTime;
    private final LocalDateTime updatedAt;
    private final KeycloakUserResponse cancellationUser;
    private final String cancelReason;

    private final String pdf;
    private final Map<String, String> formatted;

    public InvoiceResponse(final Locale locale, final Card card, final Tenant tenant,
                           final Invoice invoice, final String pdf,
                           final User keycloakUser, final User cancellationUser,
                           final List<InvoiceItemResponse> items) {
        this.uuid = invoice.getUuid();
        this.number = invoice.getNumber();
        this.card = new CardResponse(card, keycloakUser);
        this.tenant = new TenantResponse(tenant);
        this.user = new UserResponse(invoice.getUser(), keycloakUser);
        this.cancellationUser = ofNullable(cancellationUser).map(KeycloakUserResponse::new).orElse(null);
        this.pdf = pdf;

        this.cancelReason = invoice.getCancelReason();
        this.status = invoice.getPaymentStatus().getName();
        this.value = invoice.getValue();
        this.scheduledChargeTime = invoice.getScheduledChargeTime();
        this.lastChargeTime = invoice.getLastChargeTime();
        this.expirationDate = invoice.getExpirationDate();
        this.periodStart = invoice.getPeriodStart();
        this.periodEnd = invoice.getPeriodEnd();
        this.updatedAt = invoice.getUpdatedAt();
        this.paymentDate = invoice.getPaymentDate();
        this.settlementDate = invoice.getSettlementDate();
        this.createdAt = invoice.getCreatedAt();

        if (items != null) {
            this.items = items;
        } else {
            this.items = invoice.getItems().stream()
                    .map(item -> new InvoiceItemResponse(locale, item))
                    .collect(Collectors.toList());
        }

        this.formatted = new HashMap<>(4);
        this.format(locale);
    }

    public void format(final Locale locale) {
        formatted.put("value", NumberFormat.getCurrencyInstance(locale).format(value));

        ofNullable(createdAt)
                .map(date -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .ifPresent(date -> formatted.put("created_at", date));

        ofNullable(periodStart)
                .map(date -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .ifPresent(date -> formatted.put("period_start", date));

        ofNullable(periodEnd)
                .map(date -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .ifPresent(date -> formatted.put("period_end", date));

        ofNullable(scheduledChargeTime)
                .map(date -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .ifPresent(date -> formatted.put("scheduled_charge_time", date));

        ofNullable(lastChargeTime)
                .map(date -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .ifPresent(date -> formatted.put("last_charge_time", date));

        ofNullable(updatedAt)
                .map(date -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .ifPresent(date -> formatted.put("updated_at", date));
    }

}
