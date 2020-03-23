package io.gr1d.billing.api.subscriptions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Api {

    private String uuid;
    private String name;
    private String externalId;

}
