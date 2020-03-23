package io.gr1d.billing.response;

import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.spring.keycloak.model.User;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import static java.util.Optional.ofNullable;

@Getter
public class InvoiceListResponse implements Serializable {

    private final String uuid;
    private final String number;
    private final CardResponse card;
    private final TenantResponse tenant;
    private final UserResponse user;

    private final String status;
    private final BigDecimal value;
    private final LocalDate scheduledChargeTime;
    private final LocalDate expirationDate;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final LocalDate paymentDate;
    private final KeycloakUserResponse cancellationUser;
    private final String cancelReason;

    public InvoiceListResponse(final Card card, final Tenant tenant, final Invoice invoice, final User keycloakUser, final User cancellationUser) {
        this.card = new CardResponse(card, keycloakUser);
        this.tenant = new TenantResponse(tenant);
        this.user = new UserResponse(invoice.getUser(), keycloakUser);
        this.cancellationUser = ofNullable(cancellationUser).map(KeycloakUserResponse::new).orElse(null);

        this.uuid = invoice.getUuid();
        this.number = invoice.getNumber();
        this.status = invoice.getPaymentStatus().getName();
        this.value = invoice.getValue();
        this.scheduledChargeTime = invoice.getScheduledChargeTime();
        this.expirationDate = invoice.getExpirationDate();
        this.periodStart = invoice.getPeriodStart();
        this.periodEnd = invoice.getPeriodEnd();
        this.paymentDate = invoice.getPaymentDate();
        this.cancelReason = invoice.getCancelReason();
    }

}
