package io.gr1d.billing.service;

import feign.FeignException;
import io.gr1d.billing.api.subscriptions.Plan;
import io.gr1d.billing.api.subscriptions.SubscriptionsApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SubscriptionsIntegrationService {

    private final SubscriptionsApi subscriptionsApi;

    @Autowired
    public SubscriptionsIntegrationService(final SubscriptionsApi subscriptionsApi) {
        this.subscriptionsApi = subscriptionsApi;
    }

    @Cacheable("plan_data")
    public Plan findPlan(final String uuid) {
        try {
            return subscriptionsApi.findPlan(uuid);
        } catch (FeignException e) {
            log.error("Error while trying to retrieve plan data by uuid {}", uuid, e);
            return null;
        }
    }

}
