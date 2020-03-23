package io.gr1d.billing.service;

import feign.RetryableException;
import io.gr1d.auth.keycloak.LoggedUser;
import io.gr1d.billing.api.subscriptions.Plan;
import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.exception.CardNotFoundException;
import io.gr1d.billing.exception.ChargeException;
import io.gr1d.billing.exception.InvoiceNotFoundException;
import io.gr1d.billing.exception.UserNotFoundException;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.User;
import io.gr1d.billing.model.enumerations.PaymentStatus;
import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.model.invoice.InvoiceSplit;
import io.gr1d.billing.repository.InvoiceRepository;
import io.gr1d.billing.request.invoice.InvoiceCancelRequest;
import io.gr1d.billing.request.invoice.InvoiceRequest;
import io.gr1d.billing.request.invoice.InvoiceRequestItem;
import io.gr1d.billing.response.InvoiceItemResponse;
import io.gr1d.billing.response.InvoiceListResponse;
import io.gr1d.billing.response.InvoiceResponse;
import io.gr1d.billing.response.UserResponse;
import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.service.Gr1dClock;
import io.gr1d.core.upload.UploadScope;
import io.gr1d.core.upload.UploadService;
import io.gr1d.core.upload.UploadedFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
public class InvoiceService {

    private final Gr1dClock clock;
    private final CardService cardService;
    private final UserService userService;
    private final TenantService tenantService;
    private final NotificationService notificationService;
    private final ChargeService chargeService;
    private final InvoiceRepository repository;
    private final UploadService uploadService;
    private final InvoiceStatusService invoiceStatusService;
    private final KeycloakUserService keycloakUserService;
    private final SubscriptionsIntegrationService subscriptionsIntegrationService;

    public InvoiceService(final Gr1dClock clock, final CardService cardService,
                          final UserService userService, final TenantService tenantService,
                          final NotificationService notificationService,
                          final ChargeService chargeService, final InvoiceRepository repository,
                          final UploadService uploadService, final KeycloakUserService keycloakUserService,
                          final InvoiceStatusService invoiceStatusService,
                          final SubscriptionsIntegrationService subscriptionsIntegrationService) {
        this.clock = clock;
        this.cardService = cardService;
        this.userService = userService;
        this.tenantService = tenantService;
        this.notificationService = notificationService;
        this.chargeService = chargeService;
        this.repository = repository;
        this.uploadService = uploadService;
        this.keycloakUserService = keycloakUserService;
        this.invoiceStatusService = invoiceStatusService;
        this.subscriptionsIntegrationService = subscriptionsIntegrationService;
    }

    public InvoiceResponse charge(final String uuid) throws ChargeException {
        final Invoice invoice = repository.findByUuidAndRemovedAtIsNull(uuid).orElseThrow(InvoiceNotFoundException::new);
        chargeService.charge(invoice);
        return getInvoice(uuid);
    }

    private String generateInvoiceNumber(final User user, final LocalDate month) {
        final Integer seq = repository.countInvoicesByPeriod(month, user) + 1;
        final String monthText = month.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        final String seqThreeDigits = StringUtils.leftPad(seq.toString(), 3, "0");
        return String.format("%s.%s-%s", monthText, user.getId(), seqThreeDigits);
    }

    @Transactional(rollbackFor = Throwable.class)
    public InvoiceResponse create(final InvoiceRequest request) throws UserNotFoundException {
        final String userId = request.getUserId();
        final Tenant tenant = tenantService.getTenantDataByRealm(request.getTenantRealm());
        final User user = ofNullable(userService.find(tenant.getRealm(), userId))
                .orElseThrow(UserNotFoundException::new);
        final Card card = cardService.getForUser(user).orElseThrow(CardNotFoundException::new);
        final Invoice invoice = new Invoice();
        final BigDecimal totalValue = request.getItems().stream()
                .map(InvoiceRequestItem::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        invoice.setNumber(generateInvoiceNumber(user, request.getPeriodStart()));
        invoice.setCard(card);
        invoice.setUser(user);
        invoice.setValue(totalValue);
        invoice.setPaymentStatus(PaymentStatus.CREATED);
        invoice.setPeriodStart(request.getPeriodStart());
        invoice.setPeriodEnd(request.getPeriodEnd());
        invoice.setTenantRealm(tenant.getRealm());
        invoice.setScheduledChargeTime(request.getChargeDate());

        final LocalDate expirationDate = ofNullable(request.getExpirationDate())
                .orElseGet(() -> ofNullable(request.getChargeDate()).orElseGet(() -> LocalDate.now(clock)).plusDays(5));
        invoice.setExpirationDate(expirationDate);

        ofNullable(request.getItems())
                .map(its -> its.stream()
                        .map(it -> it.invoiceItem(invoice))
                        .collect(Collectors.toList()))
                .ifPresent(invoice::setItems);

        if (!CollectionUtils.isEmpty(request.getSplit())) {
            final List<InvoiceSplit> split = request.getSplit().stream()
                    .map(s -> s.toInvoiceSplit(invoice))
                    .collect(Collectors.toList());

            invoice.setSplit(split);
        }

        invoice.createUuid();
        io.gr1d.spring.keycloak.model.User keycloakUser = userService.getKeycloakUser(invoice.getUser());
        final byte[] uploadPdf = this.uploadPdf(invoice, card, new UserResponse(invoice.getUser(), keycloakUser), tenant);
        final Invoice result = repository.save(invoice);
        final InvoiceResponse response = getInvoice(invoice);

        if (isToday(result.getScheduledChargeTime())) {
            chargeService.chargeAsync(result);
        } else {
            notificationService.invoiceCreated(response, Optional.of(uploadPdf));
        }

        return response;
    }

    public PageResult<InvoiceListResponse> list(final Specification<Invoice> specification, final Pageable pageable) {
        final Page<Invoice> page = repository.findAll(specification, pageable);
        final List<InvoiceListResponse> list = page.getContent().stream().map(invoice -> {
            final Tenant tenant = tenantService.getTenantDataByRealm(invoice.getTenantRealm());
            final io.gr1d.spring.keycloak.model.User cancellationUser = ofNullable(invoice.getCancelUserId())
                    .map(keycloakUserService::getUserData).orElse(null);
            return new InvoiceListResponse(invoice.getCard(), tenant, invoice, userService.getKeycloakUser(invoice.getUser()), cancellationUser);
        }).collect(Collectors.toList());
        return PageResult.ofPage(page, list);
    }

    public InvoiceResponse getInvoice(final String uuid) {
        final Invoice invoice = repository.findByUuid(uuid)
                .orElseThrow(InvoiceNotFoundException::new);
        return getInvoice(invoice);
    }

    private InvoiceResponse getInvoice(final Invoice invoice) {
        final Locale locale = Locale.getDefault();
        final List<InvoiceItemResponse> items = invoice.getItems().stream().map(item -> {
            final Plan plan = subscriptionsIntegrationService.findPlan(item.getPlanUuid());
            return new InvoiceItemResponse(locale, item, plan);
        }).collect(Collectors.toList());
        final Tenant tenant = tenantService.getTenantDataByRealm(invoice.getTenantRealm());
        final String invoicePdf = getInvoicePdfAddress(invoice, tenant);
        final io.gr1d.spring.keycloak.model.User cancellationUser = ofNullable(invoice.getCancelUserId())
                .map(keycloakUserService::getUserData).orElse(null);
        final io.gr1d.spring.keycloak.model.User keycloakUser = userService.getKeycloakUser(invoice.getUser());
        return new InvoiceResponse(locale, invoice.getCard(), tenant,
                invoice, invoicePdf, keycloakUser, cancellationUser, items);
    }

    private String getInvoicePdfAddress(final Invoice invoice, final Tenant tenant) {
        if (StringUtils.isEmpty(invoice.getPdfFileId())) {
            final Card card = cardService.getForUser(invoice.getUser()).orElse(null);
            final UserResponse user = new UserResponse(invoice.getUser(), userService.getKeycloakUser(invoice.getUser()));
            this.uploadPdf(invoice, card, user, tenant);
            repository.save(invoice);
        }

        return ofNullable(uploadService.getUploadData(invoice.getPdfFileId()))
                .map(UploadedFile::getUrl)
                .orElse(null);
    }

    private byte[] uploadPdf(final Invoice invoice, final Card card, final UserResponse user, final Tenant tenant) {
        final byte[] pdfFile = InvoicePDFTemplate.createPdf(invoice, card, user, tenant);
        final UploadedFile uploadedFile = uploadService.upload(pdfFile, invoice.getUuid() + ".pdf", "invoices", UploadScope.PRIVATE);

        invoice.setPdfFileId(uploadedFile.getFileId());
        log.info("Created the invoice in PDF: {}", invoice.getUuid());

        return pdfFile;
    }

    private boolean isToday(final LocalDate date) {
        return date == null || date.compareTo(LocalDate.now(clock)) <= 0;
    }

    @Transactional(noRollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public void cancel(final LoggedUser loggedUser, final String uuid, final InvoiceCancelRequest request) {
        final Invoice invoice = repository.findByUuidAndPaymentStatusAndRemovedAtIsNull(uuid, PaymentStatus.CREATED).orElseThrow(InvoiceNotFoundException::new);

        invoice.setCancelReason(request.getCancelReason());
        invoice.setCancelUserId(loggedUser.getId());

        repository.save(invoice);

        invoiceStatusService.updateStatus(invoice, PaymentStatus.CANCELED);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void invoiceSettlementUpdate() throws RetryableException {
        repository.invoiceSettlementUpdate(LocalDate.now());
    }
}
