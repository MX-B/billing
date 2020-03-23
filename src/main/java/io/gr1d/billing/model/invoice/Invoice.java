package io.gr1d.billing.model.invoice;

import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.User;
import io.gr1d.billing.model.enumerations.PaymentStatus;
import io.gr1d.billing.model.transfer.TransferLetter;
import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.LAZY;

@Entity
@Getter @Setter
@EqualsAndHashCode
@Table(name = "invoice")
@EntityListeners(AuditListener.class)
public class Invoice extends BaseModel {

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String number;

    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "gateway_transaction_id", length = 64)
    private String gatewayTransactionId;

    @Column(nullable = false)
    private BigDecimal value;

    @ManyToOne
    @JoinColumn(name = "payment_status_id", nullable = false)
    private PaymentStatus paymentStatus;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "transfer_letter_id")
    private TransferLetter transferLetter;

    @OneToMany(mappedBy = "invoice", cascade = ALL, fetch = LAZY)
    private List<InvoiceItem> items;

    @OneToMany(mappedBy = "invoice", cascade = ALL, fetch = LAZY)
    private List<InvoiceSplit> split;

    @Column(name = "scheduled_charge_time")
    private LocalDate scheduledChargeTime;

    @Column(name = "last_charge_time")
    private LocalDateTime lastChargeTime;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "charge_tries")
    private int chargeTries;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "tenant_realm")
    private String tenantRealm;

    @Column(name = "pdf_file_id")
    private String pdfFileId;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "cancel_user_id")
    private String cancelUserId;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Override
    protected String uuidBase() {
        return "IN";
    }

    public void createUuid() {
        super.createUuid(true);
    }

}
