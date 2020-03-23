package io.gr1d.billing.service;

import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.exception.AddressRequiredException;
import io.gr1d.billing.exception.CardAuthorizationException;
import io.gr1d.billing.exception.CardNotFoundException;
import io.gr1d.billing.model.Address;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.DocumentType;
import io.gr1d.billing.model.User;
import io.gr1d.billing.repository.CardRepository;
import io.gr1d.billing.request.AddressRequest;
import io.gr1d.billing.request.CardAuthorizationRequest;
import io.gr1d.billing.response.CardBrand;
import io.gr1d.billing.response.CardResponse;
import io.gr1d.billing.response.TenantResponse;
import io.gr1d.billing.service.payment.PaymentStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Service
public class CardService {

    private final TenantService tenantService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final CardRepository repository;
    private final PaymentStrategy paymentStrategy;

    @Autowired
    public CardService(final TenantService tenantService, final UserService userService,
                       final NotificationService notificationService, final CardRepository repository,
                       final PaymentStrategy paymentStrategy) {
        this.tenantService = tenantService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.repository = repository;
        this.paymentStrategy = paymentStrategy;
    }

    public CardResponse getForUser(final String tenantRealm, final String userId) {
        final Tenant tenant = ofNullable(tenantService.getTenantDataByRealm(tenantRealm)).orElseThrow(CardNotFoundException::new);
        final User user = ofNullable(userService.find(tenant.getRealm(), userId)).orElseThrow(CardNotFoundException::new);
        final Card card = repository.findByUserAndRemovedAtIsNull(user).orElseThrow(CardNotFoundException::new);

        return new CardResponse(card, tenant, userService.getKeycloakUser(card.getUser()));
    }

    public Optional<Card> getForUser(final User user) {
        return repository.findByUserAndRemovedAtIsNull(user);
    }

    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public CardResponse authorizeCard(final CardAuthorizationRequest request) throws CardAuthorizationException {
        final Tenant tenant = tenantService.getTenantDataByRealm(request.getTenantRealm());
        final User user = userService.findOrCreate(request.getTenantRealm(), request.getUserId());
        Address oldAddress = null;

        if (user.getId() != null) {
            final Optional<Card> old = repository.findByUserAndRemovedAtIsNull(user);
            if (old.isPresent()) {
                final Card oldCard = old.get();
                oldCard.setRemovedAt(LocalDateTime.now());
                oldAddress = oldCard.getAddress();
                repository.save(oldCard);
            }
        }

        user.setEmail(request.getEmail());

        final Address address = request.getAddress() == null ? oldAddress : request.getAddress().toAddress();
        if (address == null) {
            throw new AddressRequiredException();
        }

        final Card card = new Card();
        card.setTenantRealm(tenant.getRealm());
        card.setUser(user);
        card.setBirthDate(request.getBirthDate());
        card.setDocument(request.getDocument());
        card.setDocumentType(DocumentType.valueOf(request.getDocumentType()));
        card.setFullName(request.getFullName());
        card.setCardHolderName(request.getCardHolderName());
        card.setPhone(request.getPhone());
        card.setCardId(paymentStrategy.authorizeCard(request));
        card.setLastDigits(stripLastDigits(request.getCardNumber()));
        card.setBrand(CardBrand.of(request.getCardNumber()));
        card.setAddress(oldAddress != null && address.hasSameConfig(oldAddress) ? oldAddress : address);

        final CardResponse response = new CardResponse(repository.save(card), tenant, userService.getKeycloakUser(card.getUser()));
        notificationService.cardAuthorized(new TenantResponse(tenant), response);

        return response;
    }

    private String stripLastDigits(final String cardNumber) {
        return cardNumber.substring(cardNumber.length() - 4);
    }

    public CardResponse updateAddressInfo(final String tenantRealm, final String userId, final AddressRequest request) {
        final Card card = ofNullable(userService.find(tenantRealm, userId))
                .map(user -> repository.findByUserAndRemovedAtIsNull(user).orElse(null))
                .orElseThrow(CardNotFoundException::new);

        card.setRemovedAt(LocalDateTime.now());
        repository.save(card);

        final Card newCard = new Card(card);
        newCard.setAddress(request.toAddress());
        repository.save(newCard);
        return getForUser(tenantRealm, userId);
    }
}
