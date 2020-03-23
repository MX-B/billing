package io.gr1d.billing.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.billing.fixtures.functions.RandomUuid;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.User;
import io.gr1d.billing.model.enumerations.PaymentStatus;
import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.model.invoice.InvoiceItem;
import io.gr1d.billing.model.invoice.InvoicePaymentStatusHistory;
import io.gr1d.billing.model.invoice.InvoiceSplit;
import io.gr1d.billing.request.invoice.InvoiceRequest;
import io.gr1d.billing.request.invoice.InvoiceRequestItem;
import io.gr1d.billing.request.invoice.InvoiceRequestSplit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class InvoiceFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(Invoice.class).addTemplate("valid", new Rule() {
            {
                add("uuid", new RandomUuid("IN-"));
                add("number", "123456789");
                add("createdAt", LocalDateTime.now());
                add("periodStart", LocalDate.now());
                add("periodEnd", LocalDate.now());
                add("expirationDate", LocalDate.now());
                add("updatedAt", null);

                add("user", one(User.class, "valid"));
                add("card", one(Card.class, "valid"));
                add("gatewayTransactionId", new RandomUuid("GATEWAY_ID-"));
                add("value", BigDecimal.valueOf(470));
                add("paymentStatus", PaymentStatus.SETTLED);
                add("items", has(3).of(InvoiceItem.class, "100", "250", "120"));
                add("tenantRealm", "tenant-realm-exclusive");
                add("paymentDate", LocalDate.now());
            }
        });

        Fixture.of(InvoiceItem.class).addTemplate("100", new Rule() {
            {
                add("uuid", new RandomUuid("II-"));
                add("createdAt", LocalDateTime.now());
                add("updatedAt", null);

                add("externalId", new RandomUuid());
                add("description", "Item description");
                add("quantity", 1);
                add("unitValue", BigDecimal.valueOf(100.00));

                add("hits", 5L);
                add("apiUuid", "API-b09b2bd1-7f62-40fe-b2da-1c4fae5bf915");
                add("planUuid", "PLAN-cd62f859-5108-4b07-8db5-0e2e2745b07a");
                add("providerUuid", "PAR-97d75353-3790-45c1-992f-3e9216b269ec");
            }
        });

        Fixture.of(InvoiceItem.class).addTemplate("250", new Rule() {
            {
                add("uuid", new RandomUuid("II-"));
                add("createdAt", LocalDateTime.now());
                add("updatedAt", null);

                add("externalId", new RandomUuid());
                add("description", "Item description");
                add("quantity", 5);
                add("unitValue", BigDecimal.valueOf(50.00));

                add("hits", 10L);
                add("endpoint", "/test1");
                add("apiUuid", "API-b09b2bd1-7f62-40fe-b2da-1c4fae5bf915");
                add("planUuid", "PLAN-cd62f859-5108-4b07-8db5-0e2e2745b07a");
                add("providerUuid", "PAR-97d75353-3790-45c1-992f-3e9216b269ec");
            }
        });

        Fixture.of(InvoiceItem.class).addTemplate("120", new Rule() {
            {
                add("uuid", new RandomUuid("II-"));
                add("createdAt", LocalDateTime.now());
                add("updatedAt", null);

                add("externalId", new RandomUuid());
                add("description", "Item description");
                add("quantity", 3);
                add("unitValue", BigDecimal.valueOf(40.00));

                add("hits", 10L);
                add("endpoint", "/test1");
                add("apiUuid", "API-236e2c1a-f823-430f-a655-ae0063c5819e");
                add("planUuid", "PLAN-a286ac89-905e-41d4-941d-6a16ae46efe0");
                add("providerUuid", "PAR-ca3d8eb9-12d7-492f-a765-1730ed1293ae");
            }
        });

        Fixture.of(Invoice.class).addTemplate("valid-withsplit-3-auto").inherits("valid", new Rule() {
            {
                add("items", has(3).of(InvoiceItem.class, "100-split", "250-split", "120-split"));
                add("split", has(3).of(InvoiceSplit.class, "250", "100", "120"));
            }
        });

        Fixture.of(Invoice.class).addTemplate("valid-withsplit-2-auto").inherits("valid", new Rule() {
            {
                add("items", has(3).of(InvoiceItem.class, "100-split", "250-split", "120-split"));
                add("split", has(2).of(InvoiceSplit.class, "120", "250"));
            }
        });

        Fixture.of(InvoiceItem.class).addTemplate("100-split", new Rule() {
            {
                add("uuid", new RandomUuid("II-"));
                add("createdAt", LocalDateTime.now());
                add("updatedAt", null);

                add("externalId", new RandomUuid());
                add("description", "Item description");
                add("quantity", 1);
                add("unitValue", BigDecimal.valueOf(100.00));

                add("hits", 5L);
                add("apiUuid", "API-bf92a19c-4c2e-44d9-921f-2ee960294444");
                add("planUuid", "PLAN-cd62f859-5108-4b07-8db5-0e2e2745b07a");
                add("providerUuid", "PRV-97d75353-3790-45c1-992f-3e9216b269ec");
            }
        });

        Fixture.of(InvoiceItem.class).addTemplate("250-split", new Rule() {
            {
                add("uuid", new RandomUuid("II-"));
                add("createdAt", LocalDateTime.now());
                add("updatedAt", null);

                add("externalId", new RandomUuid());
                add("description", "Item description");
                add("quantity", 5);
                add("unitValue", BigDecimal.valueOf(50.00));

                add("hits", 10L);
                add("endpoint", "/test1");
                add("apiUuid", "API-4965269b-8c2b-4a2a-b18f-c5ce73b1569e");
                add("planUuid", "PLAN-34247e49-3861-4d2e-a317-e0f9a22c0a1d");
                add("providerUuid", "PRV-97d75353-3790-45c1-992f-3e9216b269ec");
            }
        });

        Fixture.of(InvoiceItem.class).addTemplate("120-split", new Rule() {
            {
                add("uuid", new RandomUuid("II-"));
                add("createdAt", LocalDateTime.now());
                add("updatedAt", null);

                add("externalId", new RandomUuid());
                add("description", "Item description");
                add("quantity", 3);
                add("unitValue", BigDecimal.valueOf(40.00));

                add("hits", 10L);
                add("endpoint", "/test1");
                add("apiUuid", "API-4254c9c7-e306-404d-ad1d-172731d05417");
                add("planUuid", "PLAN-b99052b5-7b60-42c8-b036-fe2587a0080b");
                add("providerUuid", "PRV-67d62596-9bdb-4098-89d9-8118c1f12cce");
            }
        });

        Fixture.of(InvoiceSplit.class).addTemplate("100", new Rule() {
            {
                add("uuid", new RandomUuid("SPT-"));
                add("createdAt", LocalDateTime.now());
                add("updatedAt", null);

                add("apiUuid", "API-bf92a19c-4c2e-44d9-921f-2ee960294444");
                add("planUuid", "PLAN-cd62f859-5108-4b07-8db5-0e2e2745b07a");
                add("providerUuid", "PRV-97d75353-3790-45c1-992f-3e9216b269ec");

                add("providerValue", BigDecimal.valueOf(50.00));
                add("tenantValue", BigDecimal.valueOf(10.00));
            }
        });

        Fixture.of(InvoiceSplit.class).addTemplate("250", new Rule() {
            {
                add("uuid", new RandomUuid("SPT-"));
                add("createdAt", LocalDateTime.now());
                add("updatedAt", null);

                add("apiUuid", "API-4965269b-8c2b-4a2a-b18f-c5ce73b1569e");
                add("planUuid", "PLAN-34247e49-3861-4d2e-a317-e0f9a22c0a1d");
                add("providerUuid", "PRV-97d75353-3790-45c1-992f-3e9216b269ec");

                add("providerValue", BigDecimal.valueOf(100.00));
                add("tenantValue", BigDecimal.valueOf(25.00));
            }
        });

        Fixture.of(InvoiceSplit.class).addTemplate("120", new Rule() {
            {
                add("uuid", new RandomUuid("SPT-"));
                add("createdAt", LocalDateTime.now());
                add("updatedAt", null);

                add("apiUuid", "API-4254c9c7-e306-404d-ad1d-172731d05417");
                add("planUuid", "PLAN-b99052b5-7b60-42c8-b036-fe2587a0080b");
                add("providerUuid", "PRV-67d62596-9bdb-4098-89d9-8118c1f12cce");

                add("providerValue", BigDecimal.valueOf(40.00));
                add("tenantValue", BigDecimal.valueOf(20.00));
            }
        });

        Fixture.of(InvoicePaymentStatusHistory.class).addTemplate("created", new Rule() {
            {
                add("gatewayStatus", "CREATED");
                add("previousStatus", null);
                add("status", PaymentStatus.CREATED);
                add("timestamp", LocalDateTime.now());
            }
        });
        Fixture.of(InvoicePaymentStatusHistory.class).addTemplate("processing", new Rule() {
            {
                add("gatewayStatus", "PROCESSING");
                add("previousStatus", PaymentStatus.CREATED);
                add("status", PaymentStatus.PROCESSING);
                add("timestamp", LocalDateTime.now());
            }
        });
        Fixture.of(InvoicePaymentStatusHistory.class).addTemplate("success", new Rule() {
            {
                add("gatewayStatus", "PAID");
                add("previousStatus", PaymentStatus.PROCESSING);
                add("status", PaymentStatus.SUCCESS);
                add("timestamp", LocalDateTime.now());
            }
        });
        Fixture.of(InvoicePaymentStatusHistory.class).addTemplate("processingRetry", new Rule() {
            {
                add("gatewayStatus", "SOMETHING_HAPPENED_TRY_AGAIN");
                add("previousStatus", PaymentStatus.PROCESSING);
                add("status", PaymentStatus.PROCESSING_RETRY);
                add("timestamp", LocalDateTime.now());
            }
        });
        Fixture.of(InvoicePaymentStatusHistory.class).addTemplate("refunded", new Rule() {
            {
                add("gatewayStatus", "GAVE_THE_MONEY_BACK");
                add("previousStatus", PaymentStatus.REFUNDING);
                add("status", PaymentStatus.REFUNDED);
                add("timestamp", LocalDateTime.now());
            }
        });
        Fixture.of(InvoicePaymentStatusHistory.class).addTemplate("refunding", new Rule() {
            {
                add("gatewayStatus", "USER_ASKED_FOR_CASH_BACK");
                add("previousStatus", PaymentStatus.SUCCESS);
                add("status", PaymentStatus.REFUNDING);
                add("timestamp", LocalDateTime.now());
            }
        });

        Fixture.of(InvoiceRequest.class).addTemplate("valid", new Rule() {
            {
                add("periodStart", LocalDate.now());
                add("periodEnd", LocalDate.now());
                add("userId", "keycloak-user-id");
                add("items", has(2).of(InvoiceRequestItem.class, "100", "250"));
                add("tenantRealm", "tenant-realm");
            }
        });


        Fixture.of(InvoiceRequest.class).addTemplate("valid-zero-value", new Rule() {
            {
                add("periodStart", LocalDate.now());
                add("periodEnd", LocalDate.now());
                add("userId", "keycloak-user-id");
                add("items", has(1).of(InvoiceRequestItem.class, "0"));
                add("tenantRealm", "tenant-realm");
            }
        });

        Fixture.of(InvoiceRequest.class).addTemplate("valid-today", new Rule() {
            {
                add("periodStart", LocalDate.now());
                add("periodEnd", LocalDate.now());
                add("userId", "keycloak-user-id");
                add("items", has(2).of(InvoiceRequestItem.class, "100", "250"));
                add("chargeDate", LocalDate.now());
                add("expirationDate", LocalDate.now());
                add("tenantRealm", "tenant-realm");
            }
        });

        Fixture.of(InvoiceRequest.class).addTemplate("valid-scheduled").inherits("valid-today", new Rule() {
            {
                add("periodStart", LocalDate.now());
                add("periodEnd", LocalDate.now());
                add("chargeDate", LocalDate.now().plusDays(5));
                add("expirationDate", LocalDate.now().plusDays(10));
                add("tenantRealm", "tenant-realm");
            }
        });

        Fixture.of(InvoiceRequest.class).addTemplate("invalid", new Rule() {
            {
                add("periodStart", LocalDate.now());
                add("periodEnd", LocalDate.now());
                add("userId", "keycloak-user-id");
            }
        });

        Fixture.of(InvoiceRequest.class).addTemplate("valid-split").inherits("valid", new Rule() {
            {
                add("split", has(2).of(InvoiceRequestSplit.class, "valid-provider-100", "valid-provider-250"));
            }
        });

        Fixture.of(InvoiceRequestItem.class).addTemplate("0", new Rule() {
            {
                add("itemId", new RandomUuid("INVOICE_ITEM_EXTERNAL_ID-"));
                add("description", "Item description");
                add("quantity", 10);
                add("unitValue", BigDecimal.ZERO);

                add("hits", 10L);
                add("endpoint", "/");
                add("apiUuid", "API-dd7e54e8-5604-4d60-84b9-7dc1176561ec");
                add("providerUuid", "PLN-6b14dac0-953f-4b18-9d92-0743707f8fc6");
                add("planUuid", "PRV-bd542f8b-077f-483d-9f9e-3818f31e9ba7");
            }
        });

        Fixture.of(InvoiceRequestItem.class).addTemplate("100", new Rule() {
            {
                add("itemId", new RandomUuid("INVOICE_ITEM_EXTERNAL_ID-"));
                add("description", "Item description");
                add("quantity", 1);
                add("unitValue", BigDecimal.valueOf(100.00));

                add("hits", 10L);
                add("endpoint", "/");
                add("apiUuid", "API-dd7e54e8-5604-4d60-84b9-7dc1176561ec");
                add("providerUuid", "PLN-6b14dac0-953f-4b18-9d92-0743707f8fc6");
                add("planUuid", "PRV-bd542f8b-077f-483d-9f9e-3818f31e9ba7");
            }
        });

        Fixture.of(InvoiceRequestItem.class).addTemplate("250", new Rule() {
            {
                add("itemId", new RandomUuid("INVOICE_ITEM_EXTERNAL_ID-"));
                add("description", "Item description");
                add("quantity", 5);
                add("unitValue", BigDecimal.valueOf(50.00));

                add("hits", 10L);
                add("endpoint", "/test1");
                add("apiUuid", "API-fdc98475-557a-4cb7-96d4-0fc2812c60aa");
                add("providerUuid", "PLN-dbdd0423-d538-4997-9ee9-b972cf3eed34");
                add("planUuid", "PRV-db89574a-6e29-4e51-ade9-f9eab9c0f68d");
            }
        });

        Fixture.of(InvoiceRequestSplit.class).addTemplate("valid-provider-100", new Rule() {
            {
                add("apiUuid", "API-dd7e54e8-5604-4d60-84b9-7dc1176561ec");
                add("providerUuid", "PRV-bd542f8b-077f-483d-9f9e-3818f31e9ba7");
                add("planUuid", "PLN-6b14dac0-953f-4b18-9d92-0743707f8fc6");

                add("providerValue", BigDecimal.valueOf(50.00));
                add("tenantValue", BigDecimal.valueOf(10.00));
            }
        });

        Fixture.of(InvoiceRequestSplit.class).addTemplate("valid-provider-250", new Rule() {
            {
                add("apiUuid", "API-fdc98475-557a-4cb7-96d4-0fc2812c60aa");
                add("providerUuid", "PRV-db89574a-6e29-4e51-ade9-f9eab9c0f68d");
                add("planUuid", "PLN-dbdd0423-d538-4997-9ee9-b972cf3eed34");

                add("providerValue", BigDecimal.valueOf(25.00));
                add("tenantValue", BigDecimal.valueOf(5.00));
            }
        });
    }

}
