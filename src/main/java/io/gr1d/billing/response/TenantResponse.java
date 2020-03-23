package io.gr1d.billing.response;

import io.gr1d.billing.api.subscriptions.Tenant;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter @Setter
public class TenantResponse implements Serializable {

    private String name;
    private String logo;
    private String url;
    private String supportEmail;
    private String email;
    private String realm;

    public TenantResponse() {
        super();
    }

    public TenantResponse(final Tenant tenant) {
        this.name = tenant.getName();
        this.logo = tenant.getLogo();
        this.url = tenant.getUrl();
        this.supportEmail = tenant.getSupportEmail();
        this.email = tenant.getEmail();
        this.realm = tenant.getRealm();
    }
}
