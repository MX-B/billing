package io.gr1d.billing.api.recipients;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import io.gr1d.billing.api.KeycloakFeignConfiguration;
import io.gr1d.billing.api.HealthcheckApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "recipientsApi", url = "${gr1d.service.recipients}", configuration = RecipientsApi.RecipientsApiConfig.class)
public interface RecipientsApi extends HealthcheckApi {

    @GetMapping("/recipient/{recipientUuid}")
    Recipient getRecipientData(@PathVariable("recipientUuid") String recipientUuid);

    /**
     * Feign Configuration
     */
    class RecipientsApiConfig extends KeycloakFeignConfiguration {
        public RecipientsApiConfig(@Value("${keycloak.auth-server-url:${gr1d.keycloak.serviceAccount.url}}") final String baseUrl,
                                   @Value("${gr1d.keycloak.serviceAccount.realm}") final String realm,
                                   @Value("${gr1d.keycloak.serviceAccount.clientId}") final String clientId,
                                   @Value("${gr1d.keycloak.serviceAccount.clientSecret}") final String clientSecret) {

            super(baseUrl, realm, clientId, clientSecret);
        }

        @Bean
        public RequestInterceptor recipientsApiRequestInterceptor() {
            return requestInterceptor();
        }

        @Bean
        public ErrorDecoder recipientsApiErrorDecoder() {
            return errorDecoder();
        }

        @Bean
        public Logger recipientsApiLogger() {
            return logger();
        }
    }
}
