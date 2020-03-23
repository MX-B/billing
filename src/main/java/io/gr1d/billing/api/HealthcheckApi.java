package io.gr1d.billing.api;

import org.springframework.web.bind.annotation.GetMapping;

public interface HealthcheckApi {

    @GetMapping(path = "/hc")
    void healthcheck();

}
