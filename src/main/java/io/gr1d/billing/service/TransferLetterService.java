package io.gr1d.billing.service;

import io.gr1d.auth.keycloak.LoggedUser;
import io.gr1d.billing.api.subscriptions.Provider;
import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.exception.*;
import io.gr1d.billing.model.enumerations.PayableStatus;
import io.gr1d.billing.model.enumerations.PaymentStatus;
import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.model.invoice.InvoiceItem;
import io.gr1d.billing.model.invoice.InvoiceSplit;
import io.gr1d.billing.model.transfer.Payable;
import io.gr1d.billing.model.transfer.TransferLetter;
import io.gr1d.billing.model.transfer.TransferLetterProvider;
import io.gr1d.billing.model.transfer.TransferLetterTenant;
import io.gr1d.billing.repository.*;
import io.gr1d.billing.request.transfer.TransferPayableRequest;
import io.gr1d.billing.request.transfer.TransferRequest;
import io.gr1d.billing.response.*;
import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.spring.keycloak.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
public class TransferLetterService {

    private final PayableRepository payableRepository;
    private final InvoiceRepository invoiceRepository;
    private final TransferLetterRepository transferLetterRepository;
    private final TransferLetterProviderRepository transferLetterProviderRepository;
    private final TransferLetterTenantRepository transferLetterTenantRepository;
    private final ApiService apiService;
    private final KeycloakUserService keycloakUserService;
    private final TenantService tenantService;

    private ProviderService providerService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public TransferLetterService(final PayableRepository payableRepository, final InvoiceRepository invoiceRepository,
                                 final TransferLetterRepository transferLetterRepository,
                                 final TransferLetterProviderRepository transferLetterProviderRepository,
                                 final TransferLetterTenantRepository transferLetterTenantRepository,
                                 final ApiService apiService,
                                 final KeycloakUserService keycloakUserService, final TenantService tenantService) {
        this.payableRepository = payableRepository;
        this.invoiceRepository = invoiceRepository;
        this.transferLetterRepository = transferLetterRepository;
        this.transferLetterProviderRepository = transferLetterProviderRepository;
        this.transferLetterTenantRepository = transferLetterTenantRepository;
        this.apiService = apiService;
        this.keycloakUserService = keycloakUserService;
        this.tenantService = tenantService;
    }

    public PageResult<TransferLetterResponse> list(final Specification<TransferLetter> spec, final Pageable pageable) {
        final Page<TransferLetter> page = transferLetterRepository.findAll(spec, pageable);
        return PageResult.ofPage(page, page.getContent().stream().map(TransferLetterResponse::new).collect(Collectors.toList()));
    }

    public TransferLetterResponse find(final String uuid) throws TransferLetterNotFoundException {
        final TransferLetter transferLetter = transferLetterRepository.findByUuidAndRemovedAtIsNull(uuid);
        if (transferLetter == null) {
            throw new TransferLetterNotFoundException();
        }
        return new TransferLetterResponse(transferLetter);
    }

    public List<TransferLetterProviderResponse> listProviders(final String transferLetterUuid) {
        return transferLetterProviderRepository.findByTransferLetterUuid(transferLetterUuid).stream()
                .map(this::mapFrom)
                .collect(Collectors.toList());
    }

    private TransferLetterProviderResponse mapFrom(final TransferLetterProvider transferLetterProvider) {
        final Provider provider = providerService.getProviderData(transferLetterProvider.getProviderUuid());
        final User user = ofNullable(transferLetterProvider.getTransferUserId()).map(keycloakUserService::getUserData).orElse(null);
        return new TransferLetterProviderResponse(transferLetterProvider, provider, user);
    }

    public List<TransferLetterTenantResponse> listTenants(final String transferLetterUuid) {
        return transferLetterTenantRepository.findByTransferLetterUuid(transferLetterUuid).stream()
                .map(tlt -> {
                    final Tenant tenant = tenantService.getTenantDataByRealm(tlt.getTenantRealm());
                    return new TransferLetterTenantResponse(tlt, tenant);
                })
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public TransferLetterProviderResponse transfer(final LoggedUser loggedUser, final String uuid, final TransferRequest request)
            throws PayableNotFoundException, TransferLetterNotFoundException,
            PayableNotProvidedException, TransferLetterProviderNotFoundException {

        final TransferLetter transferLetter = transferLetterRepository.findByUuidAndRemovedAtIsNull(uuid);
        if (transferLetter == null) {
            throw new TransferLetterNotFoundException();
        }
        entityManager.lock(transferLetter, LockModeType.PESSIMISTIC_WRITE);

        final TransferLetterProvider transferLetterProvider = transferLetterProviderRepository
                .findByTransferLetterUuidAndProviderUuid(uuid, request.getProviderUuid());
        if (transferLetterProvider == null) {
            throw new TransferLetterProviderNotFoundException();
        }
        entityManager.lock(transferLetterProvider, LockModeType.PESSIMISTIC_WRITE);

        final List<Payable> payables = payableRepository.findByTransferLetterProvider(transferLetterProvider);
        final Map<String, Payable> map = payables.stream().collect(Collectors.toMap(Payable::getUuid, item -> item));
        final Map<String, TransferPayableRequest> requestMap = request.getTransfers()
                .stream().collect(Collectors.toMap(TransferPayableRequest::getPayableUuid, item -> item));

        final String payableNotFound = checkNotFound(request.getTransfers(), map.keySet());
        if (payableNotFound != null) {
            throw new PayableNotFoundException(payableNotFound);
        }

        final String payableNotProvided = checkNotProvided(payables, requestMap.keySet());
        if (payableNotProvided != null) {
            throw new PayableNotProvidedException(payableNotProvided);
        }

        if (!payables.isEmpty()) {
            BigDecimal transferValue = BigDecimal.ZERO;

            for (final Payable payable : payables) {
                final TransferPayableRequest payableRequest = requestMap.get(payable.getUuid());

                if (!payable.isStatusCreated()) {
                    if (payableRequest == null) {
                        continue;
                    }
                    throw new PayableAlreadyPaidException(payableRequest.getPayableUuid());
                }

                entityManager.lock(payable, LockModeType.PESSIMISTIC_WRITE);
                if (payable.getTotalValue().compareTo(payableRequest.getTransferValue()) < 0) {
                    throw new InvalidPayableTransferValueException();
                }

                transferValue = transferValue.add(payableRequest.getTransferValue());
                payable.setTransferedValue(payableRequest.getTransferValue());
                payable.setPayableStatus(PayableStatus.PAID);
                payableRepository.save(payable);
            }

            final long pendingCount = payableRepository.countPayables(transferLetter, PayableStatus.CREATED);

            transferLetter.setStatus(pendingCount == 0 ? PayableStatus.PAID : PayableStatus.IN_PROGRESS);
            transferLetter.incrementTransferedValue(transferValue);
            transferLetter.incrementProviderTransfered();

            transferLetterProvider.setStatus(PayableStatus.PAID);
            transferLetterProvider.setTransferedValue(transferValue);
            transferLetterProvider.setTransferUserId(loggedUser.getId());
            transferLetterProvider.setTransferDate(LocalDate.now());

            transferLetterRepository.save(transferLetter);
            transferLetterProviderRepository.save(transferLetterProvider);
        }

        return mapFrom(transferLetterProvider);
    }

    private InvoiceSplit lookupSplit(final Invoice invoice, final InvoiceItem item) {

        final InvoiceSplit invoiceSplit = new InvoiceSplit();
        final AtomicReference<BigDecimal> totalTenant = new AtomicReference<>(new BigDecimal(0).setScale(6));
        final AtomicReference<BigDecimal> totalProvider = new AtomicReference<>(new BigDecimal(0).setScale(6));

        invoice.getSplit().stream().filter(split -> split.getProviderUuid().equals(item.getProviderUuid())
                && split.getApiUuid().equals(item.getApiUuid())).forEach(invoiceSplit1 -> {
            totalTenant.set(totalTenant.get().add(invoiceSplit1.getTenantValue()));
            totalProvider.set(totalProvider.get().add(invoiceSplit1.getProviderValue()));
        });

        invoiceSplit.setProviderUuid(item.getProviderUuid());
        invoiceSplit.setApiUuid(item.getApiUuid());
        invoiceSplit.setInvoice(invoice);
        invoiceSplit.setPlanUuid(item.getPlanUuid());
        invoiceSplit.setTenantValue(totalTenant.get());
        invoiceSplit.setProviderValue(totalProvider.get());

        return invoiceSplit;
    }

    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public void createPayables(final LocalDate effectiveDate) {
        log.info("Looking for previous letter");
        final TransferLetter previousLetter = transferLetterRepository.findFirstByOrderByCreatedAtDesc();

        if (previousLetter != null && previousLetter.getCreatedAt().getMonthValue() == effectiveDate.getMonthValue()
                && previousLetter.getCreatedAt().getYear() == effectiveDate.getYear()) {
            log.info("Previous letter was this month, skipping");
            return;
        }

        final LocalDate startDate = previousLetter == null
                ? effectiveDate.minusMonths(3).withDayOfMonth(1)
                : previousLetter.getFinishDate().plusDays(1);
        final LocalDate finishDate = effectiveDate.minusMonths(1).withDayOfMonth(10);
        final List<Invoice> invoices = invoiceRepository.findInvoicesToTransferLetter(PaymentStatus.SETTLED, finishDate);
        final Map<String, Payable> map = new HashMap<>();
        final Map<String, TransferLetterProvider> providers = new HashMap<>();
        final Map<String, TransferLetterTenant> tenants = new HashMap<>();
        final TransferLetter transferLetter = new TransferLetter(startDate, finishDate);

        transferLetter.setCreatedAt(effectiveDate.atStartOfDay());

        log.info("Creating transfer letter with startDate({}) and finishDate({})", startDate, finishDate);
        log.info("Total of {} invoices found for this TransferLetter", invoices.size());

        invoices.forEach(invoice -> invoice.getItems().forEach(invoiceItem -> {
            log.info("Adding InvoiceItem({}) to transfer Letter", invoiceItem.getUuid());
            final Provider provider = providerService.getProviderData(invoiceItem.getProviderUuid());
            final TransferLetterProvider transferLetterProvider = providers.computeIfAbsent(invoiceItem.getProviderUuid(),
                    providerUuid -> new TransferLetterProvider(transferLetter, provider));

            final TransferLetterTenant tenant = tenants.computeIfAbsent(invoice.getTenantRealm(), this::createTenant);
            tenant.setTransferLetter(transferLetter);

            final Payable payable = map.computeIfAbsent(invoiceItem.getApiUuid(), apiUuid -> {
                final Payable p = new Payable(transferLetter, transferLetterProvider, apiUuid);
                final InvoiceSplit split = lookupSplit(invoice, invoiceItem);

                if (split != null) {
                    if (split.getProviderValue() != null && split.getProviderValue().compareTo(BigDecimal.ZERO) > 0) {
                        p.incrementTransferedValue(split.getProviderValue());
                        transferLetterProvider.incrementTransferedValue(split.getProviderValue());
                    }
                    if (split.getTenantValue() != null && split.getTenantValue().compareTo(BigDecimal.ZERO) > 0) {
                        tenant.incrementTransferedValue(split.getTenantValue());
                    }
                }

                return p;
            });

            payable.getItems().add(invoiceItem);
            payable.incrementTotalValue(invoiceItem.getValue());
            transferLetterProvider.incrementTotalValue(invoiceItem.getValue());
            tenant.incrementTotalValue(invoiceItem.getValue());
        }));

        final BigDecimal totalValue = map.values().stream()
                .map(Payable::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        transferLetter.setTotalValue(totalValue);
        transferLetter.setProviderTransfered(0);
        transferLetter.setProviderCount(providers.values().size());

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            transferLetter.setStatus(PayableStatus.PAID);
        }

        transferLetterRepository.save(transferLetter);
        tenants.values().forEach(transferLetterTenantRepository::save);
        providers.values().forEach(transferLetterProviderRepository::save);
        map.values().forEach(payableRepository::save);

        final List<Long> invoiceIds = invoices.stream().map(Invoice::getId).collect(Collectors.toList());
        if (!invoiceIds.isEmpty()) {
            invoiceRepository.updateInvoicesToTransferLetter(transferLetter.getId(), invoiceIds);
        }

        log.info("TransferLetter created with totalValue = {}", totalValue);
    }

    private TransferLetterTenant createTenant(final String tenantRealm) {
        final TransferLetterTenant newTenant = new TransferLetterTenant();
        newTenant.setTenantRealm(tenantRealm);
        newTenant.setTotalValue(BigDecimal.ZERO);
        newTenant.setTransferedValue(BigDecimal.ZERO);
        newTenant.setStatus(PayableStatus.CREATED);
        return newTenant;
    }

    private String checkNotFound(final List<TransferPayableRequest> payableRequests, final Set<String> payableUuids) {
        for (final TransferPayableRequest request : payableRequests) {
            if (!payableUuids.contains(request.getPayableUuid())) {
                return request.getPayableUuid();
            }
        }
        return null;
    }

    private String checkNotProvided(final List<Payable> payables, final Set<String> requestPayableUuids) {
        final List<Payable> createdPayables = payables.stream()
                .filter(Payable::isStatusCreated)
                .collect(Collectors.toList());
        for (final Payable payable : createdPayables) {
            if (!requestPayableUuids.contains(payable.getUuid())) {
                return payable.getUuid();
            }
        }
        return null;
    }

    public List<PayableResponse> listByProvider(final String transferLetterUuid, final String providerUuid) throws TransferLetterProviderNotFoundException {
        final TransferLetterProvider transferLetterProvider = transferLetterProviderRepository
                .findByTransferLetterUuidAndProviderUuid(transferLetterUuid, providerUuid);
        if (transferLetterProvider == null) {
            throw new TransferLetterProviderNotFoundException();
        }

        return payableRepository.findByTransferLetterProvider(transferLetterProvider)
                .stream().map(payable -> new PayableResponse(payable, apiService.getApiInfo(payable.getApiUuid())))
                .collect(Collectors.toList());
    }

    public List<PayableEndpointResponse> listEndpoints(final String payableUuid) {
        return payableRepository.listEndpoints(payableUuid);
    }

    @Autowired
    public void setProviderService(final ProviderService providerService) {
        this.providerService = providerService;
    }
}
