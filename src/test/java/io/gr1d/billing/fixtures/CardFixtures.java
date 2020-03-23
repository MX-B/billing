package io.gr1d.billing.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.function.impl.CpfFunction;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.billing.model.Address;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.DocumentType;
import io.gr1d.billing.model.User;
import io.gr1d.billing.response.CardBrand;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class CardFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(Address.class).addTemplate("valid", new Rule() {
            {
                add("city", "Campinas");
                add("country", "br");
                add("state", "São Paulo");
                add("neighborhood", "Jardim Paulistano");
                add("street", "Avenida Brigadeiro Faria Lima");
                add("zipcode", "01452905");
                add("streetNumber", "2391");
            }
        });
        Fixture.of(Card.class).addTemplate("valid", new Rule() {
            {
                add("uuid", "CE-" + UUID.randomUUID().toString());
                add("createdAt", LocalDateTime.now());
                add("updatedAt", null);

                add("user", one(User.class, "TEST-USER"));
                add("cardId", "card_11d123uc3b3");
                add("tenantRealm", "tenant-realm");
                add("lastDigits", "3598");
                add("cardHolderName", "CARD HOLDER NAME");
                add("fullName", "Card Holder Name");
                add("documentType", DocumentType.CPF);
                add("document", new CpfFunction(false));
                add("phone", "+551131589985");
                add("brand", CardBrand.VISA);
                add("birthDate", LocalDate.of(1989, 11, 1));
                add("address", one(Address.class, "valid"));
            }
        });
    }

}
