package io.gr1d.billing.controller;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.loader.FixtureFactoryLoader;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.gr1d.billing.SpringTestApplication;
import io.gr1d.billing.TestUtils;
import io.gr1d.billing.api.recipients.Recipient;
import io.gr1d.billing.api.subscriptions.Provider;
import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.model.enumerations.PaymentStatus;
import io.gr1d.billing.request.CardAuthorizationRequest;
import io.gr1d.billing.request.invoice.InvoiceCancelRequest;
import io.gr1d.billing.request.invoice.InvoiceRequest;
import io.gr1d.billing.service.ChargeScheduler;
import io.gr1d.billing.service.NotificationService;
import io.gr1d.billing.service.payment.PagarmePaymentStrategy;
import io.gr1d.billing.service.payment.PagarmeService;
import io.gr1d.billing.service.payment.PagarmeWebhookHandler;
import io.gr1d.core.email.TestEmailService;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.core.service.Gr1dClock;
import me.pagar.model.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.flywaydb.core.Flyway;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.gr1d.billing.TestUtils.*;
import static io.gr1d.billing.service.payment.PagarmePaymentStrategy.toValue;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 8099)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringTestApplication.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class InvoiceIntegrationTest {

    private static final String SIGNATURE = "128uyfd1987gfg789h23gv9384fhg893";
    private static final Integer TRANSACTION_ID = 213987417;
    private static final String CARD_ID = "card_hcudguic23g73dgcysdy";

    @Autowired private MockMvc mockMvc;
    @Autowired private Flyway flyway;
    @Autowired private PagarmePaymentStrategy pagarmePaymentStrategy;
    @Autowired private PagarmeWebhookHandler pagarmeWebhookHandler;
    @Autowired private ChargeScheduler chargeScheduler;
    @Autowired private Gr1dClock clock;

    @Mock private NotificationService notificationService;
    @Mock private PagarmeService pagarmeService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        FixtureFactoryLoader.loadTemplates("io.gr1d.billing.fixtures");

        pagarmeWebhookHandler.setPagarmeService(pagarmeService);
        pagarmePaymentStrategy.setPagarmeService(pagarmeService);

        clock.setup();
    }

    @After
    public void tearDown() {
        flyway.clean();
    }

    @Test
    @FlywayTest
    public void testInvoiceCreation() throws Exception {
        final CardAuthorizationRequest cardRequest = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final InvoiceRequest request = Fixture.from(InvoiceRequest.class).gimme("valid");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");

        mockTransaction();
        final String invoiceUuid = createInvoice(cardRequest, request, tenant);
        validateTransaction(cardRequest, request, invoiceUuid);

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("PROCESSING"));

        mockMvc.perform(get("/invoice")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].status").value("PROCESSING"));

    }

    private void configureStubForProvider(final String providerUuid, final String pagarmeRecipientId) {
        final String walletId = UUID.randomUUID().toString();
        final String providerName = "Provider Name Over here";
        final Recipient recipient = new Recipient("66072095000167", "CNPF", "Bank", "222", "4234", new HashMap<>());
        final Provider provider = new Provider(providerUuid, providerName, walletId, "phone", "email", recipient);

        createStubAuthentication();
        stubFor(WireMock.get(urlEqualTo(String.format("/provider/%s", providerUuid)))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json(provider))));

        configureStubForRecipient(walletId, pagarmeRecipientId);
    }

    private void configureStubForRecipient(final String recipientUuid, final String pagarmeRecipientId) {
        final Recipient recipient = new Recipient();
        recipient.setMetadata(Collections.singletonMap("pagarme_id", pagarmeRecipientId));

        createStubAuthentication();

        stubFor(WireMock.get(urlEqualTo(String.format("/recipient/%s", recipientUuid)))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json(recipient))));
    }

    @Test
    @FlywayTest
    public void testInvoiceCreationWithSplit() throws Exception {
        final CardAuthorizationRequest cardRequest = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final InvoiceRequest request = Fixture.from(InvoiceRequest.class).gimme("valid-split");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");

        configureStubForProvider("PRV-bd542f8b-077f-483d-9f9e-3818f31e9ba7", "provider-100-recipient-id");
        configureStubForProvider("PRV-db89574a-6e29-4e51-ade9-f9eab9c0f68d", "provider-250-recipient-id");
        configureStubForRecipient(tenant.getWalletId(), "tenant-recipient-id");
        configureStubForRecipient("REC-uuid", "owner-recipient-id");

        mockTransaction();
        final String invoiceUuid = createInvoice(cardRequest, request, tenant);
        final Transaction transaction = validateTransaction(cardRequest, request, invoiceUuid);

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("PROCESSING"));

        assertThat(transaction.getSplitRules()).isNull();
    }

    @Test
    @FlywayTest
    public void testInvoiceCreationAndUnexpectedErrorOnCharge() throws Exception {
        final CardAuthorizationRequest cardRequest = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final InvoiceRequest request = Fixture.from(InvoiceRequest.class).gimme("valid");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");
        final LocalDate chargeDate = LocalDate.now().plusDays(10);

        request.setChargeDate(chargeDate);

        final String invoiceUuid = createInvoice(cardRequest, request, tenant);

        mockExceptionTransaction();
        clock.setClock(createClock(chargeDate));
        chargeScheduler.chargeScheduledInvoices();

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("FAILED_COMMUNICATION"))
                .andExpect(jsonPath("scheduled_charge_time").value(chargeDate.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
    }

    @Test
    @FlywayTest
    public void testInvoiceCreationWithValueZero() throws Exception {
        final CardAuthorizationRequest cardRequest = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final InvoiceRequest request = Fixture.from(InvoiceRequest.class).gimme("valid-zero-value");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");
        final LocalDate chargeDate = LocalDate.now().plusDays(10);

        request.setChargeDate(chargeDate);

        final ResultActions resultActions = createInvoiceWithoutValidation(cardRequest, request, tenant);
        final String invoiceUuid = TestUtils.getResult(resultActions, CreatedResponse.class).getUuid();

        clock.setClock(createClock(chargeDate));
        chargeScheduler.chargeScheduledInvoices();

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("SUCCESS"))
                .andExpect(jsonPath("settlement_date").value(chargeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                .andExpect(jsonPath("payment_date").value(chargeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));

        verify(notificationService, times(0)).invoiceCreated(any(), any());
    }

    @Test
    @FlywayTest
    public void testInvoiceWithChargeScheduled() throws Exception {
        final CardAuthorizationRequest cardRequest = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final InvoiceRequest request = Fixture.from(InvoiceRequest.class).gimme("valid");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");
        final LocalDate chargeDate = LocalDate.now().plusDays(10);

        request.setChargeDate(chargeDate);

        final String invoiceUuid = createInvoice(cardRequest, request, tenant);

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("CREATED"));

        mockTransaction();
        clock.setClock(createClock(chargeDate));
        chargeScheduler.chargeScheduledInvoices();
        validateTransaction(cardRequest, request, invoiceUuid);

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("PROCESSING"));

        webhook(invoiceUuid, "paid");

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("SUCCESS"));
    }

    @Test
    @FlywayTest
    public void testInvoiceWithChargeScheduledAndManuallyCharge() throws Exception {
        final CardAuthorizationRequest cardRequest = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final InvoiceRequest request = Fixture.from(InvoiceRequest.class).gimme("valid");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");
        final LocalDate chargeDate = LocalDate.now().plusDays(10);

        request.setChargeDate(chargeDate);

        final String invoiceUuid = createInvoice(cardRequest, request, tenant);

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("CREATED"));

        mockTransaction();
        clock.setClock(createClock(chargeDate.minusDays(2)));
        mockMvc.perform(put(String.format("/invoice/%s/charge", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("PROCESSING"));
        validateTransaction(cardRequest, request, invoiceUuid);

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("PROCESSING"));

        webhook(invoiceUuid, "paid");

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("SUCCESS"));
    }

    @Test
    @FlywayTest
    public void testInvoiceCreationAndWebhookStatusPaid() throws Exception {
        final CardAuthorizationRequest cardRequest = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final InvoiceRequest request = Fixture.from(InvoiceRequest.class).gimme("valid");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");

        mockTransaction();
        final String invoiceUuid = createInvoice(cardRequest, request, tenant);
        validateTransaction(cardRequest, request, invoiceUuid);

        webhook(invoiceUuid, "paid");

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("SUCCESS"));
    }

    @Test
    @FlywayTest
    public void testInvoiceCreationAndWebhookStatusPaidThenRefund() throws Exception {
        final CardAuthorizationRequest cardRequest = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final InvoiceRequest request = Fixture.from(InvoiceRequest.class).gimme("valid");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");

        mockTransaction();
        final String invoiceUuid = createInvoice(cardRequest, request, tenant);
        validateTransaction(cardRequest, request, invoiceUuid);

        webhook(invoiceUuid, "paid");

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("SUCCESS"));

        webhook(invoiceUuid, "pending_refund");

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("REFUNDING"));

        webhook(invoiceUuid, "refunded");

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("REFUNDED"));
    }

    @Test
    @FlywayTest
    public void testInvoiceCreationAndWebhookStatusRefused() throws Exception {
        final CardAuthorizationRequest cardRequest = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final InvoiceRequest request = Fixture.from(InvoiceRequest.class).gimme("valid");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");

        mockTransaction();
        final String invoiceUuid = createInvoice(cardRequest, request, tenant);
        validateTransaction(cardRequest, request, invoiceUuid);

        webhook(invoiceUuid, "refused");

        final String nextChargeTime = LocalDate.now().plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("FAILED"))
                .andExpect(jsonPath("scheduled_charge_time").value(nextChargeTime));
    }

    @Test
    @FlywayTest
    public void testInvoiceCreationAndRefusedUntilBlockUserThenPaymentSucessAndUnblock() throws Exception {
        final CardAuthorizationRequest cardRequest = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final InvoiceRequest request = Fixture.from(InvoiceRequest.class).gimme("valid");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");

        mockTransaction();
        final String invoiceUuid = createInvoice(cardRequest, request, tenant);
        validateTransaction(cardRequest, request, invoiceUuid);
        webhook(invoiceUuid, "refused");

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("FAILED"));

        mockTransaction();
        mockMvc.perform(put(String.format("/invoice/%s/charge", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("PROCESSING_RETRY"));
        validateTransaction(cardRequest, request, invoiceUuid);

        webhook(invoiceUuid, "refused");
        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("FAILED"));

        stubFor(WireMock.post(urlEqualTo(String.format("/api/whitelabel/%s/user/%s/block", tenant.getRealm(), cardRequest.getUserId())))
                .willReturn(aResponse().withStatus(200)));

        mockTransaction();
        mockMvc.perform(put(String.format("/invoice/%s/charge", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("user.status").value("ACTIVE"))
                .andExpect(jsonPath("status").value("PROCESSING_RETRY"));
        validateTransaction(cardRequest, request, invoiceUuid);
        webhook(invoiceUuid, "refused");

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("user.status").value("BLOCKED"))
                .andExpect(jsonPath("status").value("FAILED"));

        stubFor(WireMock.post(urlEqualTo(String.format("/api/whitelabel/%s/user/%s/unblock", tenant.getRealm(), cardRequest.getUserId())))
                .willReturn(aResponse().withStatus(200)));

        mockTransaction();
        mockMvc.perform(put(String.format("/invoice/%s/charge", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("user.status").value("BLOCKED"))
                .andExpect(jsonPath("status").value("PROCESSING_RETRY"));
        validateTransaction(cardRequest, request, invoiceUuid);
        webhook(invoiceUuid, "paid");

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("user.status").value("ACTIVE"))
                .andExpect(jsonPath("status").value("SUCCESS"));
    }

    @Test
    @FlywayTest
    public void testInvoiceCreationAndCancel() throws Exception {
        final CardAuthorizationRequest cardRequest = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final InvoiceRequest request = Fixture.from(InvoiceRequest.class).gimme("valid");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");

        request.setChargeDate(LocalDate.now().plusDays(1));

        mockTransaction();
        final String invoiceUuid = createInvoice(cardRequest, request, tenant);

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value(PaymentStatus.CREATED.getName()))
                .andExpect(jsonPath("cancellation_user").doesNotExist())
                .andExpect(jsonPath("cancel_reason").doesNotExist());


        stubFor(WireMock.get(urlEqualTo("/admin/realms/master/users/TEST_USER"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": \"TEST_USER\",\"firstName\": \"Nathalie\",\"lastName\": \"Kinder\", \"email\": \"nathalie@company.io\"}")));

        final InvoiceCancelRequest invoiceCancelRequest = new InvoiceCancelRequest();
        invoiceCancelRequest.setCancelReason("Motivo do Cancelamento");

        mockMvc.perform(put(String.format("/invoice/%s/cancel", invoiceUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(invoiceCancelRequest))).andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value(PaymentStatus.CANCELED.getName()))
                .andExpect(jsonPath("cancellation_user.uuid").value("TEST_USER"))
                .andExpect(jsonPath("cancel_reason").value(invoiceCancelRequest.getCancelReason()));
    }

    @Test
    @FlywayTest
    public void testInvoiceCreationAndCancelFail() throws Exception {
        final CardAuthorizationRequest cardRequest = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final InvoiceRequest request = Fixture.from(InvoiceRequest.class).gimme("valid");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");

        mockTransaction();
        final String invoiceUuid = createInvoice(cardRequest, request, tenant);

        mockMvc.perform(get(String.format("/invoice/%s", invoiceUuid))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value(PaymentStatus.PROCESSING.getName()))
                .andExpect(jsonPath("cancellation_user").doesNotExist())
                .andExpect(jsonPath("cancel_reason").doesNotExist());


        stubFor(WireMock.get(urlEqualTo("/admin/realms/master/users/TEST_USER"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": \"TEST_USER\",\"firstName\": \"Nathalie\",\"lastName\": \"Kinder\", \"email\": \"nathalie@company.io\"}")));

        final InvoiceCancelRequest invoiceCancelRequest = new InvoiceCancelRequest();
        invoiceCancelRequest.setCancelReason("Motivo do Cancelamento");

        mockMvc.perform(put(String.format("/invoice/%s/cancel", invoiceUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(invoiceCancelRequest))).andDo(print())
                .andExpect(status().isNotFound());
    }

    private ResultActions createInvoiceWithoutValidation(
            final CardAuthorizationRequest cardRequest,
            final InvoiceRequest request, final Tenant tenant) throws Exception {
        authorizeCard(cardRequest, tenant);

        return mockMvc.perform(post("/invoice")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print())
                .andExpect(status().isOk());
    }

    private String createInvoice(final CardAuthorizationRequest cardRequest, final InvoiceRequest request, final Tenant tenant) throws Exception {
        authorizeCard(cardRequest, tenant);

        createStubAuthentication();
        stubFor(WireMock.get(urlEqualTo(String.format("/plan/%s", request.getItems().get(0).getPlanUuid())))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\": \"First Plan Name\"}")));
        stubFor(WireMock.get(urlEqualTo(String.format("/plan/%s", request.getItems().get(1).getPlanUuid())))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\": \"Second Plan Name\"}")));

        final ResultActions resultActions = createInvoiceWithoutValidation(cardRequest, request, tenant)
                .andExpect(jsonPath("card.last_digits").value("5212"))
                .andExpect(jsonPath("card.full_name").value(cardRequest.getFullName()))
                .andExpect(jsonPath("card.card_holder_name").value(cardRequest.getCardHolderName()))
                .andExpect(jsonPath("card.document_type").value(cardRequest.getDocumentType()))
                .andExpect(jsonPath("card.document").value(cardRequest.getDocument()))
                .andExpect(jsonPath("card.phone").value(cardRequest.getPhone()))
                .andExpect(jsonPath("card.birth_date").value(cardRequest.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                .andExpect(jsonPath("card.brand").value("mastercard"))
                .andExpect(jsonPath("user.uuid").isNotEmpty())
                .andExpect(jsonPath("user.keycloak_id").value("keycloak-user-id"))
                .andExpect(jsonPath("user.email").value(cardRequest.getEmail()))
                .andExpect(jsonPath("user.status").value("ACTIVE"))
                .andExpect(jsonPath("tenant.name").value(tenant.getName()))
                .andExpect(jsonPath("tenant.logo").value(tenant.getLogo()))
                .andExpect(jsonPath("tenant.url").value(tenant.getUrl()))
                .andExpect(jsonPath("tenant.support_email").value(tenant.getSupportEmail()))
                .andExpect(jsonPath("tenant.email").value(tenant.getEmail()))
                .andExpect(jsonPath("tenant.realm").value(tenant.getRealm()))
                .andExpect(jsonPath("status").value("CREATED"))
                .andExpect(jsonPath("number").isNotEmpty())
                .andExpect(jsonPath("items").value(hasSize(2)))
                .andExpect(jsonPath("items[0].external_id").value(request.getItems().get(0).getItemId()))
                .andExpect(jsonPath("items[0].description").value(request.getItems().get(0).getDescription()))
                .andExpect(jsonPath("items[0].quantity").value(request.getItems().get(0).getQuantity()))
                .andExpect(jsonPath("items[0].unit_value").value(request.getItems().get(0).getUnitValue()))
                .andExpect(jsonPath("items[0].endpoint").value(request.getItems().get(0).getEndpoint()))
                .andExpect(jsonPath("items[0].api_uuid").value(request.getItems().get(0).getApiUuid()))
                .andExpect(jsonPath("items[0].hits").value(request.getItems().get(0).getHits()))
                .andExpect(jsonPath("items[0].plan_uuid").value(request.getItems().get(0).getPlanUuid()))
                .andExpect(jsonPath("items[0].plan.name").value("First Plan Name"))
                .andExpect(jsonPath("items[0].provider_uuid").value(request.getItems().get(0).getProviderUuid()))
                .andExpect(jsonPath("items[1].external_id").value(request.getItems().get(1).getItemId()))
                .andExpect(jsonPath("items[1].description").value(request.getItems().get(1).getDescription()))
                .andExpect(jsonPath("items[1].quantity").value(request.getItems().get(1).getQuantity()))
                .andExpect(jsonPath("items[1].unit_value").value(request.getItems().get(1).getUnitValue()))
                .andExpect(jsonPath("items[1].endpoint").value(request.getItems().get(1).getEndpoint()))
                .andExpect(jsonPath("items[1].api_uuid").value(request.getItems().get(1).getApiUuid()))
                .andExpect(jsonPath("items[1].hits").value(request.getItems().get(1).getHits()))
                .andExpect(jsonPath("items[1].plan_uuid").value(request.getItems().get(1).getPlanUuid()))
                .andExpect(jsonPath("items[1].plan.name").value("Second Plan Name"))
                .andExpect(jsonPath("items[1].provider_uuid").value(request.getItems().get(1).getProviderUuid()));

        return TestUtils.getResult(resultActions, CreatedResponse.class).getUuid();
    }

    private Transaction validateTransaction(final CardAuthorizationRequest cardRequest, final InvoiceRequest request, final String invoiceUuid)
            throws PagarMeException, NoSuchFieldException, IllegalAccessException {
        final ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(pagarmeService, times(1)).save(captor.capture());

        // necessary because Transaction has no method `getCardId():String`
        final Field cardIdField = Transaction.class.getDeclaredField("cardId");
        cardIdField.setAccessible(true);

        final Transaction transaction = captor.getValue();
        assertThat(cardIdField.get(transaction)).isEqualTo(CARD_ID);
        assertThat(transaction.getItems()).hasSize(request.getItems().size());
//        assertThat(transaction.getSplitRules()).hasSize(invoice.getSplit().size());
        assertThat(transaction.getMetadata().get(PagarmePaymentStrategy.INVOICE_ID_METADATA)).isEqualTo(invoiceUuid);
        assertThat(transaction.getPostbackUrl()).isEqualTo("http://billing.net/webhook/handle/pagarme");

        assertThat(transaction.getBilling().getName()).isEqualTo(cardRequest.getFullName());
        assertThat(transaction.getBilling().getAddress().getStreet()).isEqualTo(cardRequest.getAddress().getStreet());
        assertThat(transaction.getBilling().getAddress().getComplementary()).isEqualTo(cardRequest.getAddress().getComplementary());
        assertThat(transaction.getBilling().getAddress().getStreetNumber()).isEqualTo(cardRequest.getAddress().getStreetNumber());
        assertThat(transaction.getBilling().getAddress().getNeighborhood()).isEqualTo(cardRequest.getAddress().getNeighborhood());
        assertThat(transaction.getBilling().getAddress().getCity()).isEqualTo(cardRequest.getAddress().getCity());
        assertThat(transaction.getBilling().getAddress().getState()).isEqualTo(cardRequest.getAddress().getState());
        assertThat(transaction.getBilling().getAddress().getZipcode()).isEqualTo(cardRequest.getAddress().getZipcode());
        assertThat(transaction.getBilling().getAddress().getCountry()).isEqualTo(cardRequest.getAddress().getCountry());

        final Iterator<Item> itemsIterator = transaction.getItems().iterator();
        final Item firstItem = itemsIterator.next();
        final Item secondItem = itemsIterator.next();
        final Integer firstValue = toValue(request.getItems().get(0).getUnitValue(), request.getItems().get(0).getQuantity());
        final Integer secondValue = toValue(request.getItems().get(1).getUnitValue(), request.getItems().get(1).getQuantity());
        assertThat(transaction.getAmount()).isEqualTo(firstValue + secondValue);

        assertThat(firstItem.getId()).isEqualTo(request.getItems().get(0).getItemId());
        assertThat(firstItem.getTitle()).isEqualTo(request.getItems().get(0).getDescription());
        assertThat(firstItem.getUnitPrice()).isEqualTo(firstValue);
        assertThat(firstItem.getQuantity()).isEqualTo(1);
        assertThat(firstItem.getTangible()).isFalse();

        assertThat(secondItem.getId()).isEqualTo(request.getItems().get(1).getItemId());
        assertThat(secondItem.getTitle()).isEqualTo(request.getItems().get(1).getDescription());
        assertThat(secondItem.getUnitPrice()).isEqualTo(secondValue);
        assertThat(secondItem.getQuantity()).isEqualTo(1);
        assertThat(secondItem.getTangible()).isFalse();

        return transaction;
    }

    private void mockTransaction() throws PagarMeException {
        when(pagarmeService.save(any(Transaction.class))).then(args -> {
            final Transaction transaction = args.getArgument(0);
            transaction.setId(TRANSACTION_ID);
            return transaction;
        });

        final Payable payable = new Payable();
        payable.setPaymentDate(new DateTime());
        when(pagarmeService.find(any(String.class)))
                .thenReturn(Collections.singletonList(payable));
    }

    private void mockExceptionTransaction() throws PagarMeException {
        when(pagarmeService.save(any(Transaction.class))).then(arg -> {
            throw new PagarMeException("Problema ao realizar pagamento");
        });
    }

    private void authorizeCard(final CardAuthorizationRequest cardRequest, final Tenant tenant) throws Exception {
        createStubAuthentication();
        stubFor(WireMock.get(urlEqualTo("/tenant/by-realm/tenant-realm"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json(tenant))));

        when(pagarmeService.save(any(Card.class))).then(args -> {
            final Card card = args.getArgument(0);
            card.setId(CARD_ID);
            final Field valid = Card.class.getDeclaredField("valid");
            valid.setAccessible(true);
            valid.set(card, true);
            return card;
        });

        mockMvc.perform(post("/card/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(cardRequest))).andDo(print())
                .andExpect(status().isOk());
    }

    private void webhook(final String invoiceUuid, final String pagarmeStatus) throws Exception {
        reset(pagarmeService);
        when(pagarmeService.validateRequestSignature(anyString(), anyString())).thenReturn(true);
        mockMvc.perform(post("/webhook/handle/pagarme")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(PagarmeWebhookHandler.SIGNATURE_HEADER, SIGNATURE)
                .content(EntityUtils.toString(new UrlEncodedFormEntity(Arrays.asList(
                        new BasicNameValuePair("current_status", pagarmeStatus),
                        new BasicNameValuePair("object", "transaction"),
                        new BasicNameValuePair("transaction[metadata][invoice_uuid]", invoiceUuid),
                        new BasicNameValuePair("transaction[metadata][request_id]", "123456")
                )))))
                .andDo(print())
                .andExpect(status().isOk());
        verify(pagarmeService, times(1)).validateRequestSignature(anyString(), eq(SIGNATURE));
    }

}
