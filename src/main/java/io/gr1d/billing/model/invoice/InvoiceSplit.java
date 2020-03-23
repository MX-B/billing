package io.gr1d.billing.model.invoice;

import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

import static javax.persistence.FetchType.LAZY;

@Getter
@Setter
@Entity
@Table(name = "invoice_split")
@EntityListeners(AuditListener.class)
public class InvoiceSplit extends BaseModel {

    @ManyToOne(optional = false, fetch = LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "provider_uuid", length = 64)
    private String providerUuid;

    @Column(name = "api_uuid", nullable = false, length = 64)
    private String apiUuid;

    @Column(name = "plan_uuid", length = 64)
    private String planUuid;

    @Column(name = "tenant_value", length = 64)
    private BigDecimal tenantValue;

    @Column(name = "provider_value", length = 64)
    private BigDecimal providerValue;

    @Override
    protected String uuidBase() {
        return "SPT";
    }

}
