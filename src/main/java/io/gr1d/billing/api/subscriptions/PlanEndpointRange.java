package io.gr1d.billing.api.subscriptions;

import lombok.Data;

import java.io.Serializable;

@Data
public class PlanEndpointRange implements Serializable {

    private Long initRange;
    private Long finalRange;
    private Long value;

}
