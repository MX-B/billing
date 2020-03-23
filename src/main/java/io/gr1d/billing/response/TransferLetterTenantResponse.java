package io.gr1d.billing.response;

import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.model.transfer.TransferLetterTenant;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class TransferLetterTenantResponse {

    private final String status;
    private final BigDecimal totalValue;
    private final BigDecimal transferedValue;
    private final TenantResponse tenant;

    public TransferLetterTenantResponse(final TransferLetterTenant transferLetterTenant, final Tenant tenant) {
        status = transferLetterTenant.getStatus().getName();
        totalValue = transferLetterTenant.getTotalValue();
        transferedValue = transferLetterTenant.getTransferedValue();

        this.tenant = new TenantResponse(tenant);
    }
}
