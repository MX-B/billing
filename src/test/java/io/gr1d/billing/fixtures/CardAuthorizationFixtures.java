package io.gr1d.billing.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.function.impl.CpfFunction;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.billing.request.AddressRequest;
import io.gr1d.billing.request.CardAuthorizationRequest;

import java.time.LocalDate;

import static io.gr1d.billing.fixtures.functions.FixtureUtils.number;

public class CardAuthorizationFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(AddressRequest.class).addTemplate("valid", new Rule() {
            {
                add("city", "São Paulo");
                add("country", "br");
                add("state", "São Paulo");
                add("neighborhood", "Jardim Paulistano");
                add("street", "Avenida Brigadeiro Faria Lima");
                add("zipcode", "01452905");
                add("streetNumber", "2391");
                add("complementary", "Apt 1001");
            }
        });
        Fixture.of(AddressRequest.class).addTemplate("another-valid", new Rule() {
            {
                add("city", "João Pessoa");
                add("country", "br");
                add("state", "Paraíba");
                add("neighborhood", "Bessa");
                add("street", "Avenida do Bessa");
                add("zipcode", "580517878");
                add("streetNumber", "1234");
                add("complementary", "Apt 1001");
            }
        });

        Fixture.of(CardAuthorizationRequest.class).addTemplate("valid", new Rule() {
            {
                add("userId", "keycloak-user-id");

                // Card
                add("cardNumber", "5130202065415212");
                add("cardExpirationDate", "0220");
                add("cardCvv", "261");
                add("cardHolderName", "TEST USER");

                // PersonalInfo
                add("fullName", "Test User");
                add("email", "test@user.com");
                add("documentType", "CPF");
                add("document", new CpfFunction(false));
                add("phone", "+5511" + number(9));
                add("birthDate", LocalDate.parse("1980-01-01"));
                add("tenantRealm", "tenant-realm");
                add("address", one(AddressRequest.class, "valid"));
            }
        });
    }
}
