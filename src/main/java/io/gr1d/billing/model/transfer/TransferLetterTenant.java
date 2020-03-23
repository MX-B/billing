package io.gr1d.billing.model.transfer;

import io.gr1d.billing.model.enumerations.PayableStatus;
import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Getter @Setter
@Table(name = "transfer_letter_tenant")
@EntityListeners(AuditListener.class)
public class TransferLetterTenant extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private PayableStatus status;

    @ManyToOne
    @JoinColumn(name = "transfer_letter_id", nullable = false)
    private TransferLetter transferLetter;

    @Column(name = "total_value")
    private BigDecimal totalValue;

    @Column(name = "transfered_value")
    private BigDecimal transferedValue;

    @Column(name = "tenant_realm")
    private String tenantRealm;

    @Override
    protected String uuidBase() {
        return "TLT";
    }

    public void incrementTotalValue(final BigDecimal value) {
        this.totalValue = this.totalValue.add(value);
    }

    public void incrementTransferedValue(final BigDecimal value) {
        this.transferedValue = this.transferedValue.add(value);
    }

}
