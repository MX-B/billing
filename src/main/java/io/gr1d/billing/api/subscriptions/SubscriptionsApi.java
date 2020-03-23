package io.gr1d.billing.api.subscriptions;

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

@FeignClient(name = "subscriptionsApi", url = "${gr1d.service.subscriptions}", configuration = SubscriptionsApi.SubscriptionsApiConfig.class)
public interface SubscriptionsApi extends HealthcheckApi {

    @GetMapping("/provider/{providerUuid}")
    Provider getProviderInfo(@PathVariable("providerUuid") String providerUuid);

    @GetMapping("/api/{apiUuid}")
    Api getApiInfo(@PathVariable("apiUuid") String apiUuid);

    @GetMapping("/tenant/by-realm/{tenantRealm}")
    Tenant getTenantInfoByRealm(@PathVariable("tenantRealm") String tenantRealm);

    @GetMapping("/plan/{uuid}")
    Plan findPlan(@PathVariable("uuid") String uuid);

    /**
     * Feign Configuration
     */
    class SubscriptionsApiConfig extends KeycloakFeignConfiguration {
        public SubscriptionsApiConfig(@Value("${keycloak.auth-server-url:${gr1d.keycloak.serviceAccount.url}}") final String baseUrl,
                                      @Value("${gr1d.keycloak.serviceAccount.realm}") final String realm,
                                      @Value("${gr1d.keycloak.serviceAccount.clientId}") final String clientId,
                                      @Value("${gr1d.keycloak.serviceAccount.clientSecret}") final String clientSecret) {

            super(baseUrl, realm, clientId, clientSecret);
        }

        @Bean
        public RequestInterceptor subscriptionsApiRequestInterceptor() {
            return requestInterceptor();
        }

        @Bean
        public ErrorDecoder subscriptionsApiErrorDecoder() {
            return errorDecoder();
        }

        @Bean
        public Logger subscriptionsApiLogger() {
            return logger();
        }
    }
}
