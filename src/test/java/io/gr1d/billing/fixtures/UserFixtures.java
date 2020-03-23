package io.gr1d.billing.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.billing.fixtures.functions.RandomUuid;
import io.gr1d.billing.model.User;
import io.gr1d.billing.model.enumerations.UserStatus;

import java.time.LocalDateTime;

public class UserFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(User.class).addTemplate("valid", new Rule() {
            {
                add("uuid", new RandomUuid("USR-"));
                add("createdAt", LocalDateTime.now());
                add("updatedAt", null);

                add("tenantRealm", "innovation-cloud");
                add("keycloakId", new RandomUuid(""));
                add("status", UserStatus.ACTIVE);
                add("email", "email@example.net");
            }
        });

        Fixture.of(User.class).addTemplate("TEST-USER", new Rule() {
            {
                add("uuid", new RandomUuid("USR-"));
                add("createdAt", LocalDateTime.now());
                add("updatedAt", null);

                add("tenantRealm", "innovation-cloud");
                add("keycloakId", new RandomUuid("TEST-USER"));
                add("status", UserStatus.ACTIVE);
                add("email", "email@example.net");
            }
        });
    }

}
