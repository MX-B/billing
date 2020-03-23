package io.gr1d.billing.api;

import feign.FeignException;
import io.gr1d.core.healthcheck.CheckService;
import io.gr1d.core.healthcheck.response.ServiceInfo;
import io.gr1d.core.healthcheck.response.enums.ServiceStatus;

public class CheckServiceApi implements CheckService {

    private final String name;
    private final HealthcheckApi service;
    private final String host;

    public CheckServiceApi(final String name, final HealthcheckApi service, final String host) {
        this.name = name;
        this.service = service;
        this.host = host;
    }

    @Override
    public ServiceInfo check() {
        try {
            service.healthcheck();
            return new ServiceInfo(name, host, ServiceStatus.LIVE, null);
        } catch (final FeignException e) {
            return new ServiceInfo(name, host, ServiceStatus.DOWN, e.getMessage());
        }
    }

}
