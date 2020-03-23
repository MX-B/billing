package io.gr1d.billing.service;

import io.gr1d.spring.keycloak.Keycloak;
import io.gr1d.spring.keycloak.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class KeycloakUserService {

    private final Keycloak keycloak;
    private final String realm;

    @Autowired
    public KeycloakUserService(final Keycloak keycloak, @Value("${keycloak.realm}") final String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    public Collection<User> search(final String realm, final String search) {
        return keycloak.searchUsers(realm, search);
    }

    @Cacheable(value = "keycloak_user", key = "#realm.#keycloakId")
    public User getUserData(final String realm, final String keycloakId) {
        return keycloak.user(realm, keycloakId);
    }

    public User getUserData(final String keycloakId) {
        return getUserData(realm, keycloakId);
    }

}
