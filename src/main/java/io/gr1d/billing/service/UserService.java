package io.gr1d.billing.service;

import feign.RetryableException;
import io.gr1d.billing.api.keychain.KeychainApi;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.User;
import io.gr1d.billing.model.enumerations.UserStatus;
import io.gr1d.billing.repository.CardRepository;
import io.gr1d.billing.repository.UserRepository;
import io.gr1d.billing.response.UserResponse;
import io.gr1d.core.datasource.model.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final KeychainApi keychainApi;
    private final KeycloakUserService keycloakUserService;
    private final CardRepository cardRepository;

    @Autowired
    public UserService(final UserRepository userRepository, final KeychainApi keychainApi,
                       final KeycloakUserService keycloakUserService, final CardRepository cardRepository) {
        this.userRepository = userRepository;
        this.keychainApi = keychainApi;
        this.keycloakUserService = keycloakUserService;
        this.cardRepository = cardRepository;
    }

    public PageResult<UserResponse> list(final Specification<Card> specification, final Pageable pageable) {
        final Page<Card> page = cardRepository.findAll(specification, pageable);
        final List<UserResponse> list = page.getContent().stream().map(UserResponse::new).collect(Collectors.toList());
        return PageResult.ofPage(page, list);
    }

    public io.gr1d.spring.keycloak.model.User getKeycloakUser(final User user) {
        return keycloakUserService.getUserData(user.getTenantRealm(), user.getKeycloakId());
    }

    public User findOrCreate(final String tenantRealm, final String keycloakId) {
        return userRepository.findByTenantRealmAndKeycloakId(tenantRealm, keycloakId)
                .orElseGet(() -> new User(tenantRealm, keycloakId));
    }

    @Cacheable(value = "users", key = "#tenantRealm.#keycloakId")
    public User find(final String tenantRealm, final String keycloakId) {
        return userRepository.findByTenantRealmAndKeycloakId(tenantRealm, keycloakId)
                .orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void blockUserSync(final User user) throws RetryableException {
        keychainApi.blockUser(user.getTenantRealm(), user.getKeycloakId());
        userRepository.blockUser(user);
        log.info("Block User sync: {}", user);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @CacheEvict(value = "users", key = "#user.tenantRealm.#user.keycloakId")
    public void blockUser(final User user) {
        try {
            blockUserSync(user);
        } catch (Exception e) {
            userRepository.blockUserPendingSync(user);
            log.error("Error sync user: {}", user, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void unblockUserSync(final User user) throws RetryableException {
        keychainApi.unblockUser(user.getTenantRealm(), user.getKeycloakId());
        userRepository.unblockUser(user);
        log.info("Unblock User sync: {}", user);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @CacheEvict(value = "users", key = "#user.tenantRealm.#user.keycloakId")
    public void unblockUser(final User user) {
        try {
            unblockUserSync(user);
        } catch (Exception e) {
            userRepository.unblockUserPendingSync(user);
            log.error("Error sync user: {}", user, e);
        }
    }

    public Iterable<User> findByUserStatusAndPendingSyncTrue(UserStatus status) {
        return userRepository.findByStatusAndPendingSyncTrue(status);
    }
}
