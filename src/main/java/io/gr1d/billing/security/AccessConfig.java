package io.gr1d.billing.security;

import io.gr1d.auth.keycloak.ConfigSecurity;
import io.gr1d.auth.keycloak.EndpointConfiguration;
import io.gr1d.billing.controller.*;
import io.gr1d.core.healthcheck.HealthCheckController;
import org.springframework.stereotype.Component;

@Component
public class AccessConfig implements EndpointConfiguration {

    private static final String USER = "user";
    private static final String ADMIN = "admin";
    private static final String TRANSFER_LETTER_READ = "transfer-letter-read";
    private static final String TRANSFER_LETTER_UPDATE = "transfer-letter-update";
    private static final String INVOICE_CANCEL = "invoice-cancel";

    @Override
    public void configure(final ConfigSecurity config) throws Exception {
        config
                .allow(HealthCheckController.class, "completeHealthCheck", ADMIN)
                .allow(CardController.class, "getCardInfo", USER, ADMIN)
                .allow(CardController.class, "authorize",  ADMIN)
                .allow(CardController.class, "updateAddressInfo", ADMIN)
                .allow(InvoiceController.class, "get", USER, ADMIN)
                .allow(InvoiceController.class, "list", USER, ADMIN)
                .allow(InvoiceController.class, "create", ADMIN)
                .allow(InvoiceController.class, "charge", ADMIN)
                .allow(InvoiceController.class, "cancel", INVOICE_CANCEL)
                .allow(UserController.class, "search", USER, ADMIN)
                .allow(UserController.class, "list", USER, ADMIN)
                .allow(UserController.class, "get", USER, ADMIN)
                .allow(TransferLetterController.class, "list", TRANSFER_LETTER_READ)
                .allow(TransferLetterController.class, "getTransferLetter", TRANSFER_LETTER_READ)
                .allow(TransferLetterController.class, "listPayableEndpoints", TRANSFER_LETTER_READ)
                .allow(TransferLetterController.class, "listByProvider", TRANSFER_LETTER_READ)
                .allow(TransferLetterController.class, "listProviders", TRANSFER_LETTER_READ)
                .allow(TransferLetterController.class, "listTenants", TRANSFER_LETTER_READ)
                .allow(TransferLetterController.class, "transfer", TRANSFER_LETTER_UPDATE)
                .allow(TransferLetterController.class, "createTransferLetterNow", TRANSFER_LETTER_UPDATE)
                .allow(CurrencyController.class, "list", USER, ADMIN)

                .allow(WebhookController.class, "handle");
    }


}
