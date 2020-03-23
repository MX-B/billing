package io.gr1d.billing.service;

import feign.FeignException;
import io.gr1d.billing.api.subscriptions.Api;
import io.gr1d.billing.api.subscriptions.SubscriptionsApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ApiService {

    private final SubscriptionsApi subscriptionsApi;

    @Autowired
    public ApiService(final SubscriptionsApi subscriptionsApi) {
        this.subscriptionsApi = subscriptionsApi;
    }

    @Cacheable("api_by_uuid")
    public Api getApiInfo(final String apiUuid) {
        log.debug("Requesting API: {}", apiUuid);
        try {
            return subscriptionsApi.getApiInfo(apiUuid);
        } catch (FeignException e) {
            log.error("Exception while trying to retrieve API info with uuid {}", apiUuid, e);
            return null;
        }
    }

}
