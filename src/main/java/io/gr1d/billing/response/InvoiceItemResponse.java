package io.gr1d.billing.response;

import io.gr1d.billing.api.subscriptions.Plan;
import io.gr1d.billing.model.invoice.InvoiceItem;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Getter
public class InvoiceItemResponse implements Serializable {

    private final String uuid;
    private final String externalId;
    private final String description;
    private final Integer quantity;
    private final BigDecimal unitValue;
    private final String endpoint;
    private final String apiUuid;
    private final Long hits;
    private final String planUuid;
    private final String providerUuid;
    private final Plan plan;

    private final Map<String, String> formatted;

    public InvoiceItemResponse(final Locale locale, final InvoiceItem invoiceItem) {
        this(locale, invoiceItem, null);
    }

    public InvoiceItemResponse(final Locale locale, final InvoiceItem invoiceItem, final Plan plan) {
        uuid = invoiceItem.getUuid();
        externalId = invoiceItem.getExternalId();
        description = invoiceItem.getDescription();
        quantity = invoiceItem.getQuantity();
        unitValue = invoiceItem.getUnitValue();
        endpoint = invoiceItem.getEndpoint();
        apiUuid = invoiceItem.getApiUuid();
        hits = invoiceItem.getHits();
        planUuid = invoiceItem.getPlanUuid();
        providerUuid = invoiceItem.getProviderUuid();
        this.plan = plan;
        formatted = new HashMap<>(5);

        format(locale);
    }

    private void format(final Locale locale) {
        final NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        format.setMaximumFractionDigits(6);

        formatted.put("quantity", NumberFormat.getIntegerInstance(locale).format(quantity));
        formatted.put("value", format.format(getValue()));
        formatted.put("unit_value", format.format(getUnitValue()));
    }

    public BigDecimal getValue() {
        return getUnitValue().multiply(BigDecimal.valueOf(getQuantity()));
    }

}
