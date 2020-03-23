package io.gr1d.billing.controller;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.loader.FixtureFactoryLoader;
import io.gr1d.billing.SpringTestApplication;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.User;
import io.gr1d.billing.repository.CardRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 8099)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringTestApplication.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class UserControllerTest {

    @Autowired private Flyway flyway;
    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;

    @Before
	public void init() {
		FixtureFactoryLoader.loadTemplates("io.gr1d.billing.fixtures");
	}

    @After
    public void tearDown() throws IllegalArgumentException {
        flyway.clean();
    }

	@Test
    @FlywayTest
	public void testListAndGetUsers() throws Exception {
        final User user = Fixture.from(User.class).gimme("valid");
        final Card card = Fixture.from(Card.class).gimme("valid");
        card.setUser(user);
        cardRepository.save(card);

        mockMvc.perform(get("/user")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].uuid").value(user.getUuid()))
                .andExpect(jsonPath("content[0].tenant_realm").value(user.getTenantRealm()))
                .andExpect(jsonPath("content[0].keycloak_id").value(user.getKeycloakId()))
                .andExpect(jsonPath("content[0].status").value(user.getStatus().getName()))
                .andExpect(jsonPath("content[0].email").value(user.getEmail()));

        mockMvc.perform(get(String.format("/user/tenant/%s/%s", user.getTenantRealm(), user.getKeycloakId()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(user.getUuid()))
                .andExpect(jsonPath("tenant_realm").value(user.getTenantRealm()))
                .andExpect(jsonPath("keycloak_id").value(user.getKeycloakId()))
                .andExpect(jsonPath("status").value(user.getStatus().getName()))
                .andExpect(jsonPath("email").value(user.getEmail()));
    }

}