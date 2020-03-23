package io.gr1d.billing.response;

import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.model.Card;
import io.gr1d.spring.keycloak.model.User;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
public class CardResponse implements Serializable {

    private TenantResponse tenant;

    private final String uuid;
    private final UserResponse user;

    private final String lastDigits;
    private final String cardHolderName;
    private final String fullName;
    private final String documentType;
    private final String document;
    private final String phone;
    private final LocalDate birthDate;
    private final String brand;
    private final AddressResponse address;

    public CardResponse(final Card card, final Tenant tenant, final User keycloakUser) {
        this(card, keycloakUser);
        this.tenant = new TenantResponse(tenant);
    }

    public CardResponse(final Card card, final User keycloakUser) {
        uuid = card.getUuid();
        user = new UserResponse(card.getUser(), keycloakUser);
        lastDigits = card.getLastDigits();
        cardHolderName = card.getCardHolderName();
        fullName = card.getFullName();
        documentType = card.getDocumentType().toString();
        document = card.getDocument();
        phone = card.getPhone();
        birthDate = card.getBirthDate();
        brand = card.getBrand().toString();
        address = new AddressResponse(card.getAddress());
    }

}

