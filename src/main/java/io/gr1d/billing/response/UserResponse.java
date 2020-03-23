package io.gr1d.billing.response;

import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.DocumentType;
import io.gr1d.billing.model.User;
import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

@Getter
public class UserResponse implements Serializable {

    private final String uuid;
    private final String tenantRealm;
    private final String keycloakId;
    private final String status;
    private final String email;
    private final String name;

    private final String firstName;
    private final String lastName;
    private final String documentType;
    private final String document;

    public UserResponse(final User user) {
        this(user, null);
    }

    public UserResponse(final User user, final io.gr1d.spring.keycloak.model.User keycloakUser) {

        final Map<String, List<String>> attributes = ofNullable(keycloakUser).map(io.gr1d.spring.keycloak.model.User::getAttributes).orElse(null);
        document = ofNullable(attributes).map(stringListMap -> ofNullable(stringListMap.get("payment_document")).orElse(Collections.singletonList("")).get(0)).orElse(null);
        if (document != null) {
            documentType = ofNullable(attributes).map(stringListMap ->
                    DocumentType.values()[new Integer(ofNullable(stringListMap.get("payment_document_type")).orElse(Collections.singletonList("2")).get(0))-1].toString()).orElse(null);
        } else {
            documentType = null;
        }

        firstName = ofNullable(keycloakUser).map(io.gr1d.spring.keycloak.model.User::getFirstName).orElse(null);
        lastName = ofNullable(keycloakUser).map(io.gr1d.spring.keycloak.model.User::getLastName).orElse(null);

        uuid = user.getUuid();
        tenantRealm = user.getTenantRealm();
        keycloakId = user.getKeycloakId();
        status = user.getStatus().getName();
        email = user.getEmail();
        name = (firstName != null) ? ((lastName != null) ? firstName.concat(" ").concat(lastName) : firstName) : null;
    }

    public UserResponse(final Card card) {

        final User user = card.getUser();

        document = card.getDocument();
        documentType = card.getDocumentType().toString();
        name = card.getFullName();
        uuid = user.getUuid();
        tenantRealm = user.getTenantRealm();
        keycloakId = user.getKeycloakId();
        status = user.getStatus().getName();
        email = user.getEmail();
        firstName = null;
        lastName = null;
    }
}
