package io.gr1d.billing.api.subscriptions;

import lombok.Data;

@Data
public class Tenant {

    private String uuid;
    private String name;
    private String walletId;
    private String logo;
    private String url;
    private String supportEmail;
    private String email;
    private String realm;

}
