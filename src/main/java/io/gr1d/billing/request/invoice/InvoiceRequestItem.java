package io.gr1d.billing.request.invoice;

import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.model.invoice.InvoiceItem;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@ToString
@Getter@Setter
public class InvoiceRequestItem {
    @NotEmpty
    @Size(max = 128)
    private String itemId;

    @NotEmpty
    @Size(max = 256)
    private String description;

    @Min(1)
    @Max(9999999)
    private int quantity;

    @Size(max = 256)
    private String endpoint;

    @Min(1)
    @Max(9999999)
    private long hits;

    @NotEmpty
    @Size(max = 256)
    private String apiUuid;

    @NotEmpty
    @Size(max = 256)
    private String planUuid;

    @NotEmpty
    @Size(max = 256)
    private String providerUuid;

    private BigDecimal unitValue;

    public BigDecimal getTotalValue() {
        return this.getUnitValue().multiply(BigDecimal.valueOf(this.getQuantity()));
    }

    public InvoiceItem invoiceItem(final Invoice invoice) {
        final InvoiceItem item = new InvoiceItem();

        item.setInvoice(invoice);
        item.setExternalId(itemId);
        item.setDescription(description);
        item.setEndpoint(endpoint);
        item.setQuantity(quantity);
        item.setUnitValue(unitValue);
        item.setApiUuid(apiUuid);
        item.setHits(hits);
        item.setPlanUuid(planUuid);
        item.setProviderUuid(providerUuid);

        return item;
    }

}
