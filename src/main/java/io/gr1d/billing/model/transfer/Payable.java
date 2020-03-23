package io.gr1d.billing.model.transfer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gr1d.billing.model.enumerations.PayableStatus;
import io.gr1d.billing.model.invoice.InvoiceItem;
import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "payable")
@EntityListeners(AuditListener.class)
public class Payable extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "transfer_letter_id")
    private TransferLetter transferLetter;

    @ManyToOne
    @JoinColumn(name = "transfer_letter_provider_id")
    private TransferLetterProvider transferLetterProvider;

    @ManyToOne
    @JoinColumn(name = "payable_status_id", nullable = false)
    private PayableStatus payableStatus;

    @Column(name = "api_uuid", length = 64)
    private String apiUuid;

    @Column(name = "total_value")
    private BigDecimal totalValue;

    @Column(name = "transfered_value")
    private BigDecimal transferedValue;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "payable_item",
            joinColumns = @JoinColumn(name = "payable_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "invoice_item_id", referencedColumnName = "id")
    )
    @JsonIgnore
    private List<InvoiceItem> items = new LinkedList<>();

    @Override
    protected String uuidBase() {
        return "PY";
    }

    public Payable(final TransferLetter transferLetter, final TransferLetterProvider provider, final String apiUuid) {
        this.transferLetter = transferLetter;
        this.payableStatus = PayableStatus.CREATED;
        this.transferedValue = BigDecimal.ZERO;
        this.totalValue = BigDecimal.ZERO;
        this.apiUuid = apiUuid;
        this.transferLetterProvider = provider;

        provider.addPayable(this);
    }

    public void incrementTransferedValue(final BigDecimal value) {
        this.transferedValue = this.transferedValue.add(value);
    }

    public void incrementTotalValue(final BigDecimal value) {
        this.totalValue = this.totalValue.add(value);
    }

    public boolean isStatusCreated() {
        return PayableStatus.CREATED.equals(payableStatus);
    }
}
