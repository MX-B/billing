package io.gr1d.billing.response;

import io.gr1d.spring.keycloak.model.User;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class KeycloakUserResponse implements Serializable {

    private final String uuid;
    private final String email;

    private final String firstName;
    private final String lastName;

    public KeycloakUserResponse(final User user) {
        uuid = user.getId();
        email = user.getEmail();
        firstName = user.getFirstName();
        lastName = user.getLastName();
    }

}
