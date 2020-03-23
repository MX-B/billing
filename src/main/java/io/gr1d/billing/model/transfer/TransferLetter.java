package io.gr1d.billing.model.transfer;

import io.gr1d.billing.model.enumerations.PayableStatus;
import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "transfer_letter")
@EntityListeners(AuditListener.class)
public class TransferLetter extends BaseModel {

    @NotNull
    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private PayableStatus status;

    @Column(name = "start_dt")
    private LocalDate startDate;

    @Column(name = "finish_dt")
    private LocalDate finishDate;

    @Column(name = "total_value")
    private BigDecimal totalValue;

    @Column(name = "transfered_value")
    private BigDecimal transferedValue;

    @Column(name = "provider_count")
    private Integer providerCount;

    @Column(name = "provider_transfered")
    private Integer providerTransfered;

    @Transient
    private List<TransferLetterProvider> providers = new LinkedList<>();

    public TransferLetter(final LocalDate startDate, final LocalDate finishDate) {
        this.startDate = startDate;
        this.finishDate = finishDate;
        this.totalValue = BigDecimal.ZERO;
        this.transferedValue = BigDecimal.ZERO;
        this.providerTransfered = 0;
        this.status = PayableStatus.CREATED;
    }

    void addProvider(final TransferLetterProvider transferLetterProvider) {
        providers.add(transferLetterProvider);
    }

    public Integer countProvidersByStatus(final PayableStatus status) {
        return (int) providers.stream()
                .filter(provider -> provider.getStatus().equals(status))
                .count();
    }

    @Override
    protected String uuidBase() {
        return "TL";
    }

    public void incrementTransferedValue(final BigDecimal value) {
        this.transferedValue = this.transferedValue.add(value);
    }

    public void incrementProviderTransfered() {
        this.providerTransfered = this.providerTransfered + 1;
    }

}
