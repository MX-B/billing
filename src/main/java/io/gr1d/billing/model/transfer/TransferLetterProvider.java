package io.gr1d.billing.model.transfer;

import io.gr1d.billing.api.subscriptions.Provider;
import io.gr1d.billing.model.enumerations.PayableStatus;
import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "transfer_letter_provider")
@EntityListeners(AuditListener.class)
public class TransferLetterProvider extends BaseModel {

    @Column(name = "transfer_user_id")
    private String transferUserId;

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

    @Column(name = "transfer_dt")
    private LocalDate transferDate;

    @Column(name = "transfered_value_auto")
    private BigDecimal transferedValueAuto;

    @Column(name = "provider_uuid")
    private String providerUuid;

    @Column(name = "gateway_transaction_id", length = 64)
    private String gatewayTransactionId;

    @OneToOne(mappedBy = "transferLetterProvider", cascade = CascadeType.ALL)
    @JoinColumn(name = "transfer_letter_provider_data_id", nullable = false)
    private TransferLetterProviderData transferLetterProviderData;

    @Transient
    private List<Payable> payables = new LinkedList<>();

    public TransferLetterProvider(final TransferLetter transferLetter, final Provider provider) {
        this.providerUuid = provider.getUuid();
        this.totalValue = BigDecimal.ZERO;
        this.transferedValue = BigDecimal.ZERO;
        this.transferedValueAuto = BigDecimal.ZERO;
        this.status = PayableStatus.CREATED;
        this.transferLetter = transferLetter;
        this.transferLetterProviderData = new TransferLetterProviderData(provider, this);

        transferLetter.addProvider(this);
    }

    void addPayable(final Payable payable) {
        payables.add(payable);
    }

    public long countPayables() {
        return payables.size();
    }

    public long countPayablesByStatus(final PayableStatus status) {
        return payables.stream()
                .filter(payable -> payable.getPayableStatus().equals(status))
                .count();
    }

    @Override
    protected String uuidBase() {
        return "TLP";
    }

    public void incrementTransferedValue(final BigDecimal value) {
        this.transferedValue = this.transferedValue.add(value);
    }

    public void incrementTotalValue(final BigDecimal value) {
        this.totalValue = this.totalValue.add(value);
    }

}
