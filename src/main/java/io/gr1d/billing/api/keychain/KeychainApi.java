package io.gr1d.billing.api.keychain;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import io.gr1d.billing.api.KeycloakFeignConfiguration;
import io.gr1d.billing.api.HealthcheckApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import static io.gr1d.core.controller.BaseController.JSON;
import static io.gr1d.core.controller.BaseController.XML;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@FeignClient(name = "keychainApi", url = "${gr1d.service.keychain}", configuration = KeychainApi.KeychainApiConfig.class)
public interface KeychainApi extends HealthcheckApi {

    @RequestMapping(path = "/api/whitelabel/{tenantRealm}/user/{keycloakId}/block", method = POST, produces = {JSON, XML})
    void blockUser(@PathVariable("tenantRealm") String tenantRealm, @PathVariable("keycloakId") String keycloakId);

    @RequestMapping(path = "/api/whitelabel/{tenantRealm}/user/{keycloakId}/unblock", method = POST, produces = {JSON, XML})
    void unblockUser(@PathVariable("tenantRealm") String tenantRealm, @PathVariable("keycloakId") String keycloakId);

    class KeychainApiConfig extends KeycloakFeignConfiguration {
        public KeychainApiConfig(@Value("${keycloak.auth-server-url:${gr1d.keycloak.serviceAccount.url}}") final String baseUrl,
                                 @Value("${gr1d.keycloak.serviceAccount.realm}") final String realm,
                                 @Value("${gr1d.keycloak.serviceAccount.clientId}") final String clientId,
                                 @Value("${gr1d.keycloak.serviceAccount.clientSecret}") final String clientSecret) {
            super(baseUrl, realm, clientId, clientSecret);
        }

        @Bean
        public RequestInterceptor userApiRequestInterceptor() {
            return requestInterceptor();
        }

        @Bean
        public ErrorDecoder userApiErrorDecoder() {
            return errorDecoder();
        }

        @Bean
        public Logger userApiLogger() {
            return logger();
        }
    }
}
