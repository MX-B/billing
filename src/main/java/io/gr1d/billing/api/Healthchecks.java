package io.gr1d.billing.api;

import io.gr1d.billing.api.keychain.KeychainApi;
import io.gr1d.billing.api.recipients.RecipientsApi;
import io.gr1d.billing.api.subscriptions.SubscriptionsApi;
import io.gr1d.core.healthcheck.CheckService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Healthchecks {

    @Bean
    CheckService recipients(final RecipientsApi api, @Value("${gr1d.service.recipients}") final String host) {
        return new CheckServiceApi("Recipients API", api, host);
    }

    @Bean
    CheckService subscriptions(final SubscriptionsApi api, @Value("${gr1d.service.subscriptions}") final String host) {
        return new CheckServiceApi("Subscriptions API", api, host);
    }

    @Bean
    CheckService recipient(final KeychainApi api, @Value("${gr1d.service.keychain}") final String host) {
        return new CheckServiceApi("Keychain API", api, host);
    }

}
