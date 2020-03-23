package io.gr1d.billing.response;

import io.gr1d.billing.api.subscriptions.Provider;
import io.gr1d.billing.model.transfer.TransferLetterProvider;
import io.gr1d.spring.keycloak.model.User;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

import static java.util.Optional.ofNullable;

@Getter
public class TransferLetterProviderResponse {

    private final String status;
    private final String providerUuid;
    private final BigDecimal totalValue;
    private final BigDecimal transferedValue;
    private final BigDecimal transferedValueAuto;
    private final Provider provider;
    private final KeycloakUserResponse user;
    private final LocalDate transferDate;

    public TransferLetterProviderResponse(final TransferLetterProvider transferLetterProvider, final Provider provider, final User keycloakUser) {
        providerUuid = transferLetterProvider.getProviderUuid();
        status = transferLetterProvider.getStatus().getName();
        totalValue = transferLetterProvider.getTotalValue();
        transferedValue = transferLetterProvider.getTransferedValue();
        transferedValueAuto = transferLetterProvider.getTransferedValueAuto();
        transferDate = transferLetterProvider.getTransferDate();
        this.provider = provider;
        this.user = ofNullable(keycloakUser).map(KeycloakUserResponse::new).orElse(null);
    }
}
