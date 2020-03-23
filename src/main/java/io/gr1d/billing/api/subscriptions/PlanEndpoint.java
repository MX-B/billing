package io.gr1d.billing.api.subscriptions;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PlanEndpoint implements Serializable {

    private String uuid;
    private String name;
    private String externalId;
    private String endpoint;
    private List<PlanEndpointRange> ranges;

}
