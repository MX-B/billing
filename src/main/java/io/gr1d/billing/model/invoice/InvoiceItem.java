package io.gr1d.billing.model.invoice;

import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Getter @Setter
@Table(name = "invoice_item")
@EntityListeners(AuditListener.class)
public class InvoiceItem extends BaseModel {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(nullable = false, length = 256)
    private String description;

    @Column
    private Integer quantity;

    @Column(name = "unit_value")
    private BigDecimal unitValue;

    @Column(nullable = false, length = 256)
    private String endpoint;

    @Column(name = "api_uuid", nullable = false)
    private String apiUuid;

    @Column(name = "hits", nullable = false)
    private Long hits;

    @Column(name = "plan_uuid", nullable = false)
    private String planUuid;

    @Column(name = "provider_uuid", nullable = false)
    private String providerUuid;

    @Override
    protected String uuidBase() {
        return "II";
    }

    public BigDecimal getValue() {
        return unitValue.multiply(BigDecimal.valueOf(quantity));
    }

}
