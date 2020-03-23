package io.gr1d.billing.api.notification;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import io.gr1d.billing.api.KeycloakFeignConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notificationApi", url = "${gr1d.service.notification}", configuration = NotificationApi.NotificationApiConfig.class)
public interface NotificationApi {

    @PostMapping("/v1/email/send")
    void sendEmail(@RequestBody EmailRequest emailRequest);

    /**
     * Feign Configuration
     */
    class NotificationApiConfig extends KeycloakFeignConfiguration {
        public NotificationApiConfig(@Value("${keycloak.auth-server-url:${gr1d.keycloak.serviceAccount.url}}") final String baseUrl,
                                     @Value("${gr1d.keycloak.serviceAccount.realm}") final String realm,
                                     @Value("${gr1d.keycloak.serviceAccount.clientId}") final String clientId,
                                     @Value("${gr1d.keycloak.serviceAccount.clientSecret}") final String clientSecret) {

            super(baseUrl, realm, clientId, clientSecret);
        }

        @Bean
        public RequestInterceptor notificationApiRequestInterceptor() {
            return requestInterceptor();
        }

        @Bean
        public ErrorDecoder notificationApiErrorDecoder() {
            return errorDecoder();
        }

        @Bean
        public Logger notificationApiLogger() {
            return logger();
        }
    }
}
