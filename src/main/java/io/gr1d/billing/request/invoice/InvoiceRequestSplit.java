package io.gr1d.billing.request.invoice;

import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.model.invoice.InvoiceSplit;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@ToString
@Getter@Setter
public class InvoiceRequestSplit {

    @NotEmpty
    @Size(max = 64)
    @ApiModelProperty(value = "This is the ID of the provider API's owner, the value will be splitted to the recipient", required = true)
    private String providerUuid;

    @NotEmpty
    @Size(max = 64)
    @ApiModelProperty(value = "This is the ID of the splitted API", required = true)
    private String apiUuid;

    @NotEmpty
    @Size(max = 64)
    @ApiModelProperty(value = "This is the ID of the current plan BUY plan", required = true)
    private String planUuid;

    @Positive
    @ApiModelProperty("If provided, a value will also be splitted between Tenant")
    private BigDecimal tenantValue;

    @Positive
    @ApiModelProperty("If provided, a value will be splitted between Provider")
    private BigDecimal providerValue;

    public InvoiceSplit toInvoiceSplit(final Invoice invoice) {
        final InvoiceSplit split = new InvoiceSplit();
        split.setInvoice(invoice);
        split.setApiUuid(apiUuid);
        split.setPlanUuid(planUuid);
        split.setProviderUuid(providerUuid);
        split.setProviderValue(providerValue == null ? BigDecimal.ZERO : providerValue);
        split.setTenantValue(tenantValue == null ? BigDecimal.ZERO : tenantValue);
        return split;
    }

}
