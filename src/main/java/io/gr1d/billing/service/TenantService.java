package io.gr1d.billing.service;

import io.gr1d.billing.api.subscriptions.SubscriptionsApi;
import io.gr1d.billing.api.subscriptions.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TenantService {

    private final SubscriptionsApi subscriptionsApi;

    @Autowired
    public TenantService(final SubscriptionsApi subscriptionsApi) {
        this.subscriptionsApi = subscriptionsApi;
    }

    @Cacheable("tenant_data_by_realm")
    public Tenant getTenantDataByRealm(final String tenantRealm) {
        log.debug("Requesting Tenant: {}", tenantRealm);
        return subscriptionsApi.getTenantInfoByRealm(tenantRealm);
    }

}
