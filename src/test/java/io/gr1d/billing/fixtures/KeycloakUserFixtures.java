package io.gr1d.billing.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.spring.keycloak.model.User;

public class KeycloakUserFixtures implements TemplateLoader {

	@Override
	public void load() {
		/*
		 * Invoice
		 */
		Fixture.of(User.class).addTemplate("valid", new Rule() {
			{
				add("id", "84a157b2-4fa7-48f9-a182-364ea5f2b6f3");
				add("enabled", true);
				add("firstName", firstName());
				add("lastName", lastName());
				add("email", "test@user.com");
			}
		});
	}
	
}
