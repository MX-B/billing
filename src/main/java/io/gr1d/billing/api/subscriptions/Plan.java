package io.gr1d.billing.api.subscriptions;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Plan implements Serializable {

    private String uuid;
    private String modality;
    private String name;
    private String description;
    private Long value;
    private List<PlanEndpoint> planEndpoints;

}
