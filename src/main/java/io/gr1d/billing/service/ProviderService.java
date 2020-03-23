package io.gr1d.billing.service;

import feign.FeignException;
import io.gr1d.billing.api.subscriptions.Provider;
import io.gr1d.billing.api.subscriptions.SubscriptionsApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProviderService {

    private final SubscriptionsApi subscriptionsApi;

    @Autowired
    public ProviderService(final SubscriptionsApi subscriptionsApi) {
        this.subscriptionsApi = subscriptionsApi;
    }

    @Cacheable("provider_data")
    public Provider getProviderData(final String providerUuid) {
        log.debug("Requesting Provider: {}", providerUuid);
        try {
            return subscriptionsApi.getProviderInfo(providerUuid);
        } catch (FeignException e) {
            log.error("Exception while trying to retrieve provider info with uuid {}", providerUuid, e);
            return null;
        }

    }

}
