package io.gr1d.billing.response;


import io.gr1d.billing.api.subscriptions.Api;
import io.gr1d.billing.model.transfer.Payable;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PayableResponse {

    private final String uuid;
    private final Api api;
    private final String providerUuid;
    private final BigDecimal totalValue;
    private final BigDecimal transferedValue;
    private final String status;

    public PayableResponse(final Payable payable, final Api api) {
        this.api = api;

        uuid = payable.getUuid();
        providerUuid = payable.getTransferLetterProvider().getProviderUuid();
        totalValue = payable.getTotalValue();
        transferedValue = payable.getTransferedValue();
        status = payable.getPayableStatus().getName();
    }

}
