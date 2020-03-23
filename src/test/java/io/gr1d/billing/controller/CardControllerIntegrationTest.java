package io.gr1d.billing.controller;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.loader.FixtureFactoryLoader;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.gr1d.billing.SpringTestApplication;
import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.request.AddressRequest;
import io.gr1d.billing.request.CardAuthorizationRequest;
import io.gr1d.billing.service.NotificationService;
import io.gr1d.billing.service.payment.PagarmePaymentStrategy;
import io.gr1d.billing.service.payment.PagarmeService;
import me.pagar.model.Card;
import org.flywaydb.core.Flyway;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
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

import java.lang.reflect.Field;
import java.time.format.DateTimeFormatter;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.gr1d.billing.TestUtils.createStubAuthentication;
import static io.gr1d.billing.TestUtils.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 8099)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringTestApplication.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class CardControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private Flyway flyway;
    @Autowired private PagarmePaymentStrategy pagarmePaymentStrategy;

    @Mock private PagarmeService pagarmeService;
    @Mock private NotificationService notificationService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        FixtureFactoryLoader.loadTemplates("io.gr1d.billing.fixtures");

        pagarmePaymentStrategy.setPagarmeService(pagarmeService);
    }

    @After
    public void tearDown() {
        flyway.clean();
    }

    @Test
    @FlywayTest
    public void testCardAuthorization() throws Exception {
        final CardAuthorizationRequest request = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");
        final String uri = "/card/authorize";

        verify(notificationService, times(0)).invoiceCreated(any(), any());

        request.setTenantRealm("tenant-realm");

        createStubAuthentication();
        stubFor(WireMock.get(urlEqualTo("/tenant/by-realm/tenant-realm"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json(tenant))));

        when(pagarmeService.save(any(Card.class))).then(args -> {
            final Card card = args.getArgument(0);
            card.setId("2139874176941");
            final Field valid = Card.class.getDeclaredField("valid");
            valid.setAccessible(true);
            valid.set(card, true);
            return card;
        });
        mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("last_digits").value("5212"))
                .andExpect(jsonPath("full_name").value(request.getFullName()))
                .andExpect(jsonPath("card_holder_name").value(request.getCardHolderName()))
                .andExpect(jsonPath("document_type").value(request.getDocumentType()))
                .andExpect(jsonPath("document").value(request.getDocument()))
                .andExpect(jsonPath("phone").value(request.getPhone()))
                .andExpect(jsonPath("birth_date").value(request.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                .andExpect(jsonPath("brand").value("mastercard"))
                .andExpect(jsonPath("user.uuid").isNotEmpty())
                .andExpect(jsonPath("user.keycloak_id").value("keycloak-user-id"))
                .andExpect(jsonPath("user.email").value(request.getEmail()))
                .andExpect(jsonPath("user.status").value("ACTIVE"))
                .andExpect(jsonPath("tenant.name").value(tenant.getName()))
                .andExpect(jsonPath("tenant.logo").value(tenant.getLogo()))
                .andExpect(jsonPath("tenant.url").value(tenant.getUrl()))
                .andExpect(jsonPath("tenant.support_email").value(tenant.getSupportEmail()))
                .andExpect(jsonPath("tenant.email").value(tenant.getEmail()))
                .andExpect(jsonPath("tenant.realm").value(tenant.getRealm()))
                .andExpect(jsonPath("address.street").value(request.getAddress().getStreet()))
                .andExpect(jsonPath("address.neighborhood").value(request.getAddress().getNeighborhood()))
                .andExpect(jsonPath("address.street_number").value(request.getAddress().getStreetNumber()))
                .andExpect(jsonPath("address.state").value(request.getAddress().getState()))
                .andExpect(jsonPath("address.city").value(request.getAddress().getCity()))
                .andExpect(jsonPath("address.country").value(request.getAddress().getCountry()))
                .andExpect(jsonPath("address.zipcode").value(request.getAddress().getZipcode()))
        ;

        final String cardInfoUri = String.format("/card/tenant/%s/user/%s", tenant.getRealm(), request.getUserId());
        mockMvc.perform(get(cardInfoUri))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("last_digits").value("5212"))
                .andExpect(jsonPath("full_name").value(request.getFullName()))
                .andExpect(jsonPath("card_holder_name").value(request.getCardHolderName()))
                .andExpect(jsonPath("document_type").value(request.getDocumentType()))
                .andExpect(jsonPath("document").value(request.getDocument()))
                .andExpect(jsonPath("phone").value(request.getPhone()))
                .andExpect(jsonPath("birth_date").value(request.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                .andExpect(jsonPath("brand").value("mastercard"))
                .andExpect(jsonPath("user.uuid").isNotEmpty())
                .andExpect(jsonPath("user.keycloak_id").value("keycloak-user-id"))
                .andExpect(jsonPath("user.email").value(request.getEmail()))
                .andExpect(jsonPath("user.status").value("ACTIVE"))
                .andExpect(jsonPath("tenant.name").value(tenant.getName()))
                .andExpect(jsonPath("tenant.logo").value(tenant.getLogo()))
                .andExpect(jsonPath("tenant.url").value(tenant.getUrl()))
                .andExpect(jsonPath("tenant.support_email").value(tenant.getSupportEmail()))
                .andExpect(jsonPath("tenant.email").value(tenant.getEmail()))
                .andExpect(jsonPath("tenant.realm").value(tenant.getRealm()))
                .andExpect(jsonPath("address.street").value(request.getAddress().getStreet()))
                .andExpect(jsonPath("address.neighborhood").value(request.getAddress().getNeighborhood()))
                .andExpect(jsonPath("address.street_number").value(request.getAddress().getStreetNumber()))
                .andExpect(jsonPath("address.state").value(request.getAddress().getState()))
                .andExpect(jsonPath("address.city").value(request.getAddress().getCity()))
                .andExpect(jsonPath("address.country").value(request.getAddress().getCountry()))
                .andExpect(jsonPath("address.zipcode").value(request.getAddress().getZipcode()));

        verify(notificationService, times(0)).invoiceCreated(any(), any());

        final ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(pagarmeService, times(1)).save(captor.capture());

        final Card requestCard = captor.getValue();
        assertThat(requestCard.getHolderName()).isEqualTo(request.getCardHolderName());
        assertThat(requestCard.getNumber()).isEqualTo(request.getCardNumber());
        assertThat(requestCard.getCvv()).isEqualTo(Integer.valueOf(request.getCardCvv()));
        assertThat(requestCard.getExpiresAt()).isEqualTo(request.getCardExpirationDate());

        request.setAddress(null);
        mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print())
                .andExpect(status().isOk());

        final AddressRequest newAddress = Fixture.from(AddressRequest.class).gimme("another-valid");
        mockMvc.perform(post(String.format("%s/address", cardInfoUri))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(newAddress))).andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get(cardInfoUri))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("last_digits").value("5212"))
                .andExpect(jsonPath("full_name").value(request.getFullName()))
                .andExpect(jsonPath("card_holder_name").value(request.getCardHolderName()))
                .andExpect(jsonPath("document_type").value(request.getDocumentType()))
                .andExpect(jsonPath("document").value(request.getDocument()))
                .andExpect(jsonPath("phone").value(request.getPhone()))
                .andExpect(jsonPath("birth_date").value(request.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                .andExpect(jsonPath("brand").value("mastercard"))
                .andExpect(jsonPath("user.uuid").isNotEmpty())
                .andExpect(jsonPath("user.keycloak_id").value("keycloak-user-id"))
                .andExpect(jsonPath("user.email").value(request.getEmail()))
                .andExpect(jsonPath("user.status").value("ACTIVE"))
                .andExpect(jsonPath("tenant.name").value(tenant.getName()))
                .andExpect(jsonPath("tenant.logo").value(tenant.getLogo()))
                .andExpect(jsonPath("tenant.url").value(tenant.getUrl()))
                .andExpect(jsonPath("tenant.support_email").value(tenant.getSupportEmail()))
                .andExpect(jsonPath("tenant.email").value(tenant.getEmail()))
                .andExpect(jsonPath("tenant.realm").value(tenant.getRealm()))
                .andExpect(jsonPath("address.street").value(newAddress.getStreet()))
                .andExpect(jsonPath("address.complementary").value(newAddress.getComplementary()))
                .andExpect(jsonPath("address.neighborhood").value(newAddress.getNeighborhood()))
                .andExpect(jsonPath("address.street_number").value(newAddress.getStreetNumber()))
                .andExpect(jsonPath("address.state").value(newAddress.getState()))
                .andExpect(jsonPath("address.city").value(newAddress.getCity()))
                .andExpect(jsonPath("address.country").value(newAddress.getCountry()))
                .andExpect(jsonPath("address.zipcode").value(newAddress.getZipcode()));
    }

    @Test
    @FlywayTest
    public void testAddressRequired() throws Exception {
        final CardAuthorizationRequest request = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");
        final String uri = "/card/authorize";

        request.setAddress(null);
        request.setTenantRealm("tenant-realm");

        createStubAuthentication();
        stubFor(WireMock.get(urlEqualTo("/tenant/by-realm/tenant-realm"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json(tenant))));

        mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$[0].meta").value("address"))
                .andExpect(jsonPath("$[0].error").value("ObjectError"));

    }

    @Test
    @FlywayTest
    public void testUnauthorizedCard() throws Exception {
        final CardAuthorizationRequest request = Fixture.from(CardAuthorizationRequest.class).gimme("valid");
        final Tenant tenant = Fixture.from(Tenant.class).gimme("valid");
        final String uri = "/card/authorize";

        verify(notificationService, times(0)).invoiceCreated(any(), any());

        request.setTenantRealm("tenant-realm");

        createStubAuthentication();
        stubFor(WireMock.get(urlEqualTo("/tenant/by-realm/tenant-realm"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json(tenant))));

        when(pagarmeService.save(any(Card.class))).then(args -> {
            final Card card = args.getArgument(0);
            card.setId("2139874176941");
            final Field valid = Card.class.getDeclaredField("valid");
            valid.setAccessible(true);
            valid.set(card, false); // cartao invalido
            return card;
        });
        mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print())
                .andExpect(status().isMethodFailure())
        ;

    }

}
