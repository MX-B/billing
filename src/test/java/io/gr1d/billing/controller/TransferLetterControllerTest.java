package io.gr1d.billing.controller;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.loader.FixtureFactoryLoader;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.gr1d.billing.SpringTestApplication;
import io.gr1d.billing.TestUtils;
import io.gr1d.billing.api.recipients.Recipient;
import io.gr1d.billing.api.subscriptions.Api;
import io.gr1d.billing.api.subscriptions.Provider;
import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.User;
import io.gr1d.billing.model.enumerations.PaymentStatus;
import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.model.invoice.InvoiceItem;
import io.gr1d.billing.model.transfer.TransferLetter;
import io.gr1d.billing.repository.InvoiceRepository;
import io.gr1d.billing.repository.TransferLetterRepository;
import io.gr1d.billing.request.transfer.TransferPayableRequest;
import io.gr1d.billing.request.transfer.TransferRequest;
import io.gr1d.billing.service.ProviderService;
import io.gr1d.billing.service.TransferLetterService;
import io.gr1d.billing.util.CardTestService;
import io.gr1d.core.model.CreatedResponse;
import org.flywaydb.core.Flyway;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.gr1d.billing.TestUtils.configureStubForTenant;
import static io.gr1d.billing.TestUtils.json;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 8099)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringTestApplication.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class TransferLetterControllerTest {

    @Autowired private CardTestService cardService;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private TransferLetterRepository transferLetterRepository;
    @Autowired private TransferLetterService transferLetterService;
    @Autowired private Flyway flyway;
    @Autowired private MockMvc mockMvc;
    @Mock private ProviderService providerService;

    private Invoice invoice;
    private final Recipient recipient = new Recipient("66072095000167", "CNPF", "Bank", "222", "4234", new HashMap<>());
    private final Provider provider = new Provider("", "Provider Name Over here", "a-random-wallet-id", "123453", "provider@email.com", recipient);

    @Before
	public void init() {
        MockitoAnnotations.initMocks(this);
		FixtureFactoryLoader.loadTemplates("io.gr1d.billing.fixtures");
        transferLetterService.setProviderService(providerService);
	}

    @After
    public void tearDown() throws IllegalArgumentException {
        flyway.clean();
    }

	@Test
    @FlywayTest
	public void testTransferLetterCreation() {
	    assertThat(transferLetterRepository.count()).isEqualTo(0);
        transferLetterService.createPayables(LocalDate.of(2018, 10, 15));
        assertThat(transferLetterRepository.count()).isEqualTo(1);

        // should not create again
        transferLetterService.createPayables(LocalDate.of(2018, 10, 16));
        assertThat(transferLetterRepository.count()).isEqualTo(1);

        TransferLetter transferLetter = transferLetterRepository.findFirstByOrderByCreatedAtDesc();
        assertThat(transferLetter.getTotalValue()).isZero();
        assertThat(transferLetter.getTransferedValue()).isZero();
        assertThat(transferLetter.getStartDate()).isEqualTo(LocalDate.of(2018, 7, 1));
        assertThat(transferLetter.getFinishDate()).isEqualTo(LocalDate.of(2018, 9, 10));

        transferLetterService.createPayables(LocalDate.of(2018, 11, 15));
        assertThat(transferLetterRepository.count()).isEqualTo(2);

        transferLetter = transferLetterRepository.findFirstByOrderByCreatedAtDesc();
        assertThat(transferLetter.getTotalValue()).isZero();
        assertThat(transferLetter.getTransferedValue()).isZero();
        assertThat(transferLetter.getStartDate()).isEqualTo(LocalDate.of(2018, 9, 11));
        assertThat(transferLetter.getFinishDate()).isEqualTo(LocalDate.of(2018, 10, 10));
    }

    @Test
    @FlywayTest
    public void testTransferLetterCreationWithPayable() throws Exception {
        final String transferLetterUuid = createTransferLetter(Fixture.from(Invoice.class).gimme("valid"));
        final Api api = configureStubForApi();

        mockMvc.perform(get("/transfer-letter")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].status").value("CREATED"))
                .andExpect(jsonPath("content[0].total_value").value(470.00))
                .andExpect(jsonPath("content[0].start_date").value("2018-07-01"))
                .andExpect(jsonPath("content[0].finish_date").value("2018-09-10"))
                .andExpect(jsonPath("content[0].provider_count").value(2))
                .andExpect(jsonPath("content[0].provider_transfered").value(0))
                .andExpect(jsonPath("content[0].transfered_value").value(0.00));

        mockMvc.perform(get(String.format("/transfer-letter/%s", transferLetterUuid))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("CREATED"))
                .andExpect(jsonPath("total_value").value(470.00))
                .andExpect(jsonPath("start_date").value("2018-07-01"))
                .andExpect(jsonPath("finish_date").value("2018-09-10"))
                .andExpect(jsonPath("provider_count").value(2))
                .andExpect(jsonPath("provider_transfered").value(0))
                .andExpect(jsonPath("transfered_value").value(0.00));

        mockMvc.perform(get("/transfer-letter/asdasdasdas")).andDo(print())
                .andExpect(status().isNotFound());

        mockMvc.perform(get(String.format("/transfer-letter/%s/provider", transferLetterUuid))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[0].total_value").value(350.00))
                .andExpect(jsonPath("$[0].provider_uuid").value(invoice.getItems().get(0).getProviderUuid()))
                .andExpect(jsonPath("$[0].provider.name").value(provider.getName()))
                .andExpect(jsonPath("$[0].transfered_value").value(0.00))
                .andExpect(jsonPath("$[1].status").value("CREATED"))
                .andExpect(jsonPath("$[1].total_value").value(120.00))
                .andExpect(jsonPath("$[1].provider_uuid").value(invoice.getItems().get(2).getProviderUuid()))
                .andExpect(jsonPath("$[1].provider.name").value(provider.getName()))
                .andExpect(jsonPath("$[1].transfered_value").value(0.00));

        final String providerUuid = invoice.getItems().get(0).getProviderUuid();
        final ResultActions resultActions = mockMvc.perform(get(String.format("/transfer-letter/%s/provider/%s", transferLetterUuid, providerUuid))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].api.uuid").value(api.getUuid()))
                .andExpect(jsonPath("$[0].api.name").value(api.getName()))
                .andExpect(jsonPath("$[0].total_value").value(350.00))
                .andExpect(jsonPath("$[0].transfered_value").value(0.00))
                .andExpect(jsonPath("$[0].provider_uuid").value(providerUuid))
                .andExpect(jsonPath("$[0].status").value("CREATED"));
        final CreatedResponse [] payableResult = TestUtils.getResult(resultActions, CreatedResponse[].class);

        mockMvc.perform(get(String.format("/transfer-letter/payable/%s/endpoints", payableResult[0].getUuid()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].endpoint").value("/test1"))
                .andExpect(jsonPath("$[0].unit_value").value(50.00))
                .andExpect(jsonPath("$[0].quantity").value(5));

        mockMvc.perform(get(String.format("/transfer-letter/%s/provider/%s", transferLetterUuid, "provider-not-found"))).andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @FlywayTest
    public void testTransferLetterCreationWithAutoOnly() throws Exception {
        final String transferLetterUuid = createTransferLetter(Fixture.from(Invoice.class).gimme("valid-withsplit-3-auto"));
        invoice.getItems().sort(comparing(InvoiceItem::getValue));

        final Api api100 = TestUtils.configureStubForApi(invoice.getItems().get(0).getApiUuid(), "My Second API", "my-second-api");
        final Api api120 = TestUtils.configureStubForApi(invoice.getItems().get(1).getApiUuid(), "Thy First API", "thy-first-api");
        final Api api250 = TestUtils.configureStubForApi(invoice.getItems().get(2).getApiUuid(), "My First API", "my-first-api");
        final Tenant tenant = configureStubForTenant(invoice.getTenantRealm(), "Tenant Cool Name");

        mockMvc.perform(get("/transfer-letter")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].status").value("CREATED"))
                .andExpect(jsonPath("content[0].total_value").value(470.00))
                .andExpect(jsonPath("content[0].start_date").value("2018-07-01"))
                .andExpect(jsonPath("content[0].finish_date").value("2018-09-10"))
                .andExpect(jsonPath("content[0].provider_count").value(2))
                .andExpect(jsonPath("content[0].provider_transfered").value(0))
                .andExpect(jsonPath("content[0].transfered_value").value(0.00));

        mockMvc.perform(get(String.format("/transfer-letter/%s/provider", transferLetterUuid))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[0].total_value").value(350.00))
                .andExpect(jsonPath("$[0].provider_uuid").value(invoice.getItems().get(0).getProviderUuid()))
                .andExpect(jsonPath("$[0].provider.name").value(provider.getName()))
                .andExpect(jsonPath("$[0].transfered_value").value(150.00))
                .andExpect(jsonPath("$[1].status").value("CREATED"))
                .andExpect(jsonPath("$[1].total_value").value(120.00))
                .andExpect(jsonPath("$[1].provider_uuid").value(invoice.getItems().get(1).getProviderUuid()))
                .andExpect(jsonPath("$[1].provider.name").value(provider.getName()))
                .andExpect(jsonPath("$[1].transfered_value").value(40.00));

        mockMvc.perform(get(String.format("/transfer-letter/%s/tenant", transferLetterUuid))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[0].tenant.realm").value(tenant.getRealm()))
                .andExpect(jsonPath("$[0].tenant.name").value(tenant.getName()))
                .andExpect(jsonPath("$[0].tenant.email").value(tenant.getEmail()))
                .andExpect(jsonPath("$[0].total_value").value(470.00))
                .andExpect(jsonPath("$[0].transfered_value").value(55.00));

        final String providerUuid = invoice.getItems().get(0).getProviderUuid();
        mockMvc.perform(get(String.format("/transfer-letter/%s/provider/%s", transferLetterUuid, providerUuid))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(2)))
                .andExpect(jsonPath("$[0].api.uuid").value(api250.getUuid()))
                .andExpect(jsonPath("$[0].api.name").value(api250.getName()))
                .andExpect(jsonPath("$[0].total_value").value(250.00))
                .andExpect(jsonPath("$[0].transfered_value").value(100.00))
                .andExpect(jsonPath("$[0].provider_uuid").value(providerUuid))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[1].api.uuid").value(api100.getUuid()))
                .andExpect(jsonPath("$[1].api.name").value(api100.getName()))
                .andExpect(jsonPath("$[1].total_value").value(100.00))
                .andExpect(jsonPath("$[1].transfered_value").value(50.00))
                .andExpect(jsonPath("$[1].provider_uuid").value(providerUuid))
                .andExpect(jsonPath("$[1].status").value("CREATED"));

        final String secondProviderUuid = invoice.getItems().get(1).getProviderUuid();
        mockMvc.perform(get(String.format("/transfer-letter/%s/provider/%s", transferLetterUuid, secondProviderUuid))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].api.uuid").value(api120.getUuid()))
                .andExpect(jsonPath("$[0].api.name").value(api120.getName()))
                .andExpect(jsonPath("$[0].total_value").value(120.00))
                .andExpect(jsonPath("$[0].transfered_value").value(40.00))
                .andExpect(jsonPath("$[0].provider_uuid").value(secondProviderUuid))
                .andExpect(jsonPath("$[0].status").value("CREATED"));
    }

    @Test
    @FlywayTest
    public void testTransferLetterCreationWithAutoAndManual() throws Exception {
        final String transferLetterUuid = createTransferLetter(Fixture.from(Invoice.class).gimme("valid-withsplit-2-auto"));
        invoice.getItems().sort(comparing(InvoiceItem::getValue));

        final Api api100 = TestUtils.configureStubForApi(invoice.getItems().get(0).getApiUuid(), "My Second API", "my-second-api");
        final Api api120 = TestUtils.configureStubForApi(invoice.getItems().get(1).getApiUuid(), "Thy First API", "thy-first-api");
        final Api api250 = TestUtils.configureStubForApi(invoice.getItems().get(2).getApiUuid(), "My First API", "my-first-api");

        mockMvc.perform(get("/transfer-letter")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].status").value("CREATED"))
                .andExpect(jsonPath("content[0].total_value").value(470.00))
                .andExpect(jsonPath("content[0].start_date").value("2018-07-01"))
                .andExpect(jsonPath("content[0].finish_date").value("2018-09-10"))
                .andExpect(jsonPath("content[0].provider_count").value(2))
                .andExpect(jsonPath("content[0].provider_transfered").value(0))
                .andExpect(jsonPath("content[0].transfered_value").value(0));

        mockMvc.perform(get(String.format("/transfer-letter/%s/provider", transferLetterUuid))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[0].total_value").value(350.00))
                .andExpect(jsonPath("$[0].provider_uuid").value(invoice.getItems().get(0).getProviderUuid()))
                .andExpect(jsonPath("$[0].provider.name").value(provider.getName()))
                .andExpect(jsonPath("$[0].transfered_value").value(100.00))
                .andExpect(jsonPath("$[1].status").value("CREATED"))
                .andExpect(jsonPath("$[1].total_value").value(120.00))
                .andExpect(jsonPath("$[1].provider_uuid").value(invoice.getItems().get(1).getProviderUuid()))
                .andExpect(jsonPath("$[1].provider.name").value(provider.getName()))
                .andExpect(jsonPath("$[1].transfered_value").value(40.00));

        final ResultActions resultActions = mockMvc.perform(get(String.format("/transfer-letter/%s/provider/%s", transferLetterUuid, invoice.getItems().get(0).getProviderUuid())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(2)))
                .andExpect(jsonPath("$[0].api.uuid").value(api250.getUuid()))
                .andExpect(jsonPath("$[0].api.name").value(api250.getName()))
                .andExpect(jsonPath("$[0].total_value").value(250.00))
                .andExpect(jsonPath("$[0].transfered_value").value(100.00))
                .andExpect(jsonPath("$[0].provider_uuid").value(invoice.getItems().get(0).getProviderUuid()))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[1].api.uuid").value(api100.getUuid()))
                .andExpect(jsonPath("$[1].api.name").value(api100.getName()))
                .andExpect(jsonPath("$[1].total_value").value(100.00))
                .andExpect(jsonPath("$[1].transfered_value").value(0.00))
                .andExpect(jsonPath("$[1].provider_uuid").value(invoice.getItems().get(0).getProviderUuid()))
                .andExpect(jsonPath("$[1].status").value("CREATED"));

        final String secondProviderUuid = invoice.getItems().get(1).getProviderUuid();
        mockMvc.perform(get(String.format("/transfer-letter/%s/provider/%s", transferLetterUuid, secondProviderUuid))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].api.uuid").value(api120.getUuid()))
                .andExpect(jsonPath("$[0].api.name").value(api120.getName()))
                .andExpect(jsonPath("$[0].total_value").value(120.00))
                .andExpect(jsonPath("$[0].transfered_value").value(40.00))
                .andExpect(jsonPath("$[0].provider_uuid").value(secondProviderUuid))
                .andExpect(jsonPath("$[0].status").value("CREATED"));

        final CreatedResponse [] payableResult = TestUtils.getResult(resultActions, CreatedResponse[].class);
        final BigDecimal transferValue = BigDecimal.valueOf(36.50);
        final BigDecimal transferValue2 = BigDecimal.valueOf(85.60);
        final TransferPayableRequest transferPayableRequest = new TransferPayableRequest(payableResult[0].getUuid(), transferValue);
        final TransferPayableRequest transferPayableRequest2 = new TransferPayableRequest(payableResult[1].getUuid(), transferValue2);
        final TransferRequest transferRequest = new TransferRequest(invoice.getItems().get(0).getProviderUuid(),
                Arrays.asList(transferPayableRequest, transferPayableRequest2));

        mockMvc.perform(post(String.format("/transfer-letter/%s/transfer", transferLetterUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(transferRequest))).andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format("/transfer-letter/%s/provider/%s", transferLetterUuid, invoice.getItems().get(0).getProviderUuid())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(2)))
                .andExpect(jsonPath("$[0].api.uuid").value(api250.getUuid()))
                .andExpect(jsonPath("$[0].api.name").value(api250.getName()))
                .andExpect(jsonPath("$[0].total_value").value(250.00))
                .andExpect(jsonPath("$[0].transfered_value").value(transferValue))
                .andExpect(jsonPath("$[0].provider_uuid").value(invoice.getItems().get(0).getProviderUuid()))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[1].api.uuid").value(api100.getUuid()))
                .andExpect(jsonPath("$[1].api.name").value(api100.getName()))
                .andExpect(jsonPath("$[1].total_value").value(100.00))
                .andExpect(jsonPath("$[1].transfered_value").value(transferValue2))
                .andExpect(jsonPath("$[1].provider_uuid").value(invoice.getItems().get(0).getProviderUuid()))
                .andExpect(jsonPath("$[1].status").value("PAID"));

        mockMvc.perform(get("/transfer-letter")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("content[0].total_value").value(470.00))
                .andExpect(jsonPath("content[0].start_date").value("2018-07-01"))
                .andExpect(jsonPath("content[0].finish_date").value("2018-09-10"))
                .andExpect(jsonPath("content[0].provider_count").value(2))
                .andExpect(jsonPath("content[0].provider_transfered").value(1))
                .andExpect(jsonPath("content[0].transfered_value").value(transferValue.add(transferValue2)));
    }

    @Test
    @FlywayTest
    public void testTransferLetterCreationAndTransfer() throws Exception {
        final String transferLetterUuid = createTransferLetter(Fixture.from(Invoice.class).gimme("valid"));
        final Provider providerOne = TestUtils.configureStubForProvider(invoice.getItems().get(0).getProviderUuid());
        final Provider providerTwo = TestUtils.configureStubForProvider(invoice.getItems().get(2).getProviderUuid());

        configureStubForApi();

        final ResultActions resultActions = mockMvc.perform(get(String.format("/transfer-letter/%s/provider/%s", transferLetterUuid, providerOne.getUuid()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[0].total_value").value(350.00))
                .andExpect(jsonPath("$[0].transfered_value").value(0.00));

        final CreatedResponse [] providerOneResult = TestUtils.getResult(resultActions, CreatedResponse[].class);
        final BigDecimal transferValue = BigDecimal.valueOf(105.90);
        final TransferPayableRequest transferPayableRequest = new TransferPayableRequest(providerOneResult[0].getUuid(), transferValue);
        final TransferRequest transferRequest = new TransferRequest(providerOne.getUuid(), singletonList(transferPayableRequest));

        stubFor(WireMock.get(urlEqualTo("/admin/realms/master/users/TEST_USER"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": \"TEST_USER\",\"firstName\": \"Nathalie\",\"lastName\": \"Kinder\", \"email\": \"nathalie@company.io\"}")));

        mockMvc.perform(post(String.format("/transfer-letter/%s/transfer", transferLetterUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(transferRequest))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("PAID"))
                .andExpect(jsonPath("total_value").value(350.00))
                .andExpect(jsonPath("provider_uuid").value(invoice.getItems().get(0).getProviderUuid()))
                .andExpect(jsonPath("transfered_value").value(105.90))
                .andExpect(jsonPath("transfer_date").exists())
                .andExpect(jsonPath("user.uuid").value("TEST_USER"))
                .andExpect(jsonPath("user.email").value("nathalie@company.io"))
                .andExpect(jsonPath("user.first_name").value("Nathalie"))
                .andExpect(jsonPath("user.last_name").value("Kinder"));

        mockMvc.perform(get(String.format("/transfer-letter/%s/provider/%s", transferLetterUuid, providerOne.getUuid()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].total_value").value(350.00))
                .andExpect(jsonPath("$[0].transfered_value").value(105.90));

        mockMvc.perform(get("/transfer-letter")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("content[0].total_value").value(470.00))
                .andExpect(jsonPath("content[0].start_date").value("2018-07-01"))
                .andExpect(jsonPath("content[0].finish_date").value("2018-09-10"))
                .andExpect(jsonPath("content[0].provider_count").value(2))
                .andExpect(jsonPath("content[0].provider_transfered").value(1))
                .andExpect(jsonPath("content[0].transfered_value").value(105.90));

        mockMvc.perform(get(String.format("/transfer-letter/%s/provider", transferLetterUuid))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[0].total_value").value(120.00))
                .andExpect(jsonPath("$[0].provider_uuid").value(invoice.getItems().get(2).getProviderUuid()))
                .andExpect(jsonPath("$[0].transfered_value").value(0.00))
                .andExpect(jsonPath("$[0].user").doesNotExist())
                .andExpect(jsonPath("$[1].status").value("PAID"))
                .andExpect(jsonPath("$[1].total_value").value(350.00))
                .andExpect(jsonPath("$[1].provider_uuid").value(invoice.getItems().get(0).getProviderUuid()))
                .andExpect(jsonPath("$[1].transfered_value").value(105.90))
                .andExpect(jsonPath("$[1].transfer_date").exists())
                .andExpect(jsonPath("$[1].user.uuid").value("TEST_USER"))
                .andExpect(jsonPath("$[1].user.email").value("nathalie@company.io"))
                .andExpect(jsonPath("$[1].user.first_name").value("Nathalie"))
                .andExpect(jsonPath("$[1].user.last_name").value("Kinder"));

        mockMvc.perform(post(String.format("/transfer-letter/%s/transfer", transferLetterUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(transferRequest))).andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].error").value("ExceptionWithStatus"));

        final ResultActions providerTwoResultActions = mockMvc.perform(get(String.format("/transfer-letter/%s/provider/%s", transferLetterUuid, providerTwo.getUuid()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[0].total_value").value(120.00))
                .andExpect(jsonPath("$[0].transfered_value").value(0.00));

        final CreatedResponse [] providerTwoResult = TestUtils.getResult(providerTwoResultActions, CreatedResponse[].class);
        final TransferPayableRequest providerTwoTransferPayableRequest = new TransferPayableRequest(providerTwoResult[0].getUuid(), BigDecimal.ZERO);
        final TransferRequest providerTwoTransferRequest = new TransferRequest(providerTwo.getUuid(), singletonList(providerTwoTransferPayableRequest));

        mockMvc.perform(post(String.format("/transfer-letter/%s/transfer", transferLetterUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(providerTwoTransferRequest))).andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format("/transfer-letter/%s/provider/%s", transferLetterUuid, providerTwo.getUuid()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].total_value").value(120.00))
                .andExpect(jsonPath("$[0].transfered_value").value(0.00));

        mockMvc.perform(get("/transfer-letter")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].status").value("PAID"))
                .andExpect(jsonPath("content[0].total_value").value(470.00))
                .andExpect(jsonPath("content[0].start_date").value("2018-07-01"))
                .andExpect(jsonPath("content[0].finish_date").value("2018-09-10"))
                .andExpect(jsonPath("content[0].provider_count").value(2))
                .andExpect(jsonPath("content[0].provider_transfered").value(2))
                .andExpect(jsonPath("content[0].transfered_value").value(105.90));

        mockMvc.perform(get(String.format("/transfer-letter/%s/provider", transferLetterUuid))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].total_value").value(350.00))
                .andExpect(jsonPath("$[0].provider_uuid").value(invoice.getItems().get(0).getProviderUuid()))
                .andExpect(jsonPath("$[0].transfered_value").value(105.90))
                .andExpect(jsonPath("$[0].user.uuid").value("TEST_USER"))
                .andExpect(jsonPath("$[0].user.email").value("nathalie@company.io"))
                .andExpect(jsonPath("$[0].user.first_name").value("Nathalie"))
                .andExpect(jsonPath("$[0].user.last_name").value("Kinder"))
                .andExpect(jsonPath("$[1].status").value("PAID"))
                .andExpect(jsonPath("$[1].total_value").value(120.00))
                .andExpect(jsonPath("$[1].provider_uuid").value(invoice.getItems().get(2).getProviderUuid()))
                .andExpect(jsonPath("$[1].transfered_value").value(0.00))
                .andExpect(jsonPath("$[1].user.uuid").value("TEST_USER"))
                .andExpect(jsonPath("$[1].user.email").value("nathalie@company.io"))
                .andExpect(jsonPath("$[1].user.first_name").value("Nathalie"))
                .andExpect(jsonPath("$[1].user.last_name").value("Kinder"));
    }

    @Test
    @FlywayTest
    public void testTransferValidations() throws Exception {
        final String transferLetterUuid = createTransferLetter(Fixture.from(Invoice.class).gimme("valid"));
        final String providerUuid = invoice.getItems().get(0).getProviderUuid();
        configureStubForApi();

        final ResultActions resultActions = mockMvc.perform(get(String.format("/transfer-letter/%s/provider/%s", transferLetterUuid, providerUuid))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[0].total_value").value(350.00))
                .andExpect(jsonPath("$[0].transfered_value").value(0.00));

        final CreatedResponse [] result = TestUtils.getResult(resultActions, CreatedResponse[].class);
        final BigDecimal transferValue = BigDecimal.valueOf(350.01);
        final TransferPayableRequest transferPayableRequest = new TransferPayableRequest(result[0].getUuid(), transferValue);
        final TransferRequest transferRequest = new TransferRequest(providerUuid, singletonList(transferPayableRequest));

        // invalid value
        mockMvc.perform(post(String.format("/transfer-letter/%s/transfer", transferLetterUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(transferRequest))).andDo(print())
                .andExpect(status().isUnprocessableEntity());

        transferPayableRequest.setTransferValue(BigDecimal.TEN);
        transferPayableRequest.setPayableUuid("invalid-uuid");

        // payable not found
        mockMvc.perform(post(String.format("/transfer-letter/%s/transfer", transferLetterUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(transferRequest))).andDo(print())
                .andExpect(status().isUnprocessableEntity());

        transferRequest.setTransfers(new ArrayList<>());

        // payable not provided
        mockMvc.perform(post(String.format("/transfer-letter/%s/transfer", transferLetterUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(transferRequest))).andDo(print())
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @FlywayTest
    public void createTransferLetterWithRefundedInvoice() {
        invoice = Fixture.from(Invoice.class).gimme("valid");
        final User user = Fixture.from(User.class).gimme("valid");
        final Card card = invoice.getCard();

        card.setUser(user);
        cardService.save(card);

        invoice.setPaymentDate(LocalDate.of(2018, 9, 5));
        invoice.setUser(user);
        invoice.setPaymentStatus(PaymentStatus.REFUNDED);
        invoiceRepository.save(invoice);

        assertThat(transferLetterRepository.count()).isEqualTo(0);
        transferLetterService.createPayables(LocalDate.of(2018, 10, 15));
        assertThat(transferLetterRepository.count()).isEqualTo(1);

        final TransferLetter transferLetter = transferLetterRepository.findFirstByOrderByCreatedAtDesc();
        assertThat(transferLetter.getTotalValue().doubleValue()).isEqualTo(0.0);
        assertThat(transferLetter.getTransferedValue()).isZero();
    }

    private String createTransferLetter(final Invoice invoice) {

        final Answer<Provider> answer = invocation -> {
            String providerUuid = invocation.getArgument(0);
            provider.setUuid(providerUuid);
            return provider;
        };

        when(providerService.getProviderData(anyString())).thenAnswer(answer);

        this.invoice = invoice;
        final User user = Fixture.from(User.class).gimme("valid");
        final Card card = invoice.getCard();

        card.setUser(user);
        cardService.save(card);

        invoice.setPaymentDate(LocalDate.of(2018, 9, 5));
        invoice.setUser(user);
        invoiceRepository.save(invoice);

        assertThat(transferLetterRepository.count()).isEqualTo(0);
        transferLetterService.createPayables(LocalDate.of(2018, 10, 15));
        assertThat(transferLetterRepository.count()).isEqualTo(1);

        transferLetterService.createPayables(LocalDate.of(2018, 10, 15));
        assertThat(transferLetterRepository.count()).isEqualTo(1);

        final TransferLetter transferLetter = transferLetterRepository.findFirstByOrderByCreatedAtDesc();
        assertThat(transferLetter.getTotalValue().doubleValue()).isEqualTo(invoice.getValue().doubleValue());

        return transferLetter.getUuid();
    }

    private Api configureStubForApi() {
        final String apiName = "Api Name Over here";
        final String apiUuid = invoice.getItems().get(0).getApiUuid();

        return TestUtils.configureStubForApi(apiUuid, apiName, "external-id-over-here");
    }

}