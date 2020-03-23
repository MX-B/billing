package io.gr1d.billing.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.fixtures.functions.RandomUuid;
import io.gr1d.billing.response.TenantResponse;

public class TenantResponseFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(TenantResponse.class).addTemplate("valid", new Rule() {
            {
                add("name", "Tenant Name");
                add("logo", "http://www.test.com/image.png");
                add("url", "http://www.test.com");
                add("supportEmail", "support@tenant.com");
                add("email", "admin@tenant.com");
            }
        });

        Fixture.of(Tenant.class).addTemplate("valid", new Rule() {
            {
                add("uuid", new RandomUuid());
                add("name", "Tenant Name");
                add("logo", "http://www.test.com/image.png");
                add("url", "http://www.test.com");
                add("supportEmail", "support@tenant.com");
                add("email", "admin@tenant.com");
                add("realm", "tenant-realm");
                add("walletId", "tenant-wallet-id");
            }
        });
    }

}
