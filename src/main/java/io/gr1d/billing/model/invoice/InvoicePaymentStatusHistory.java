package io.gr1d.billing.model.invoice;

import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.enumerations.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Getter @Setter
@Table(name = "invoice_payment_status_history")
public class InvoicePaymentStatusHistory {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(optional = false, fetch = LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "previous_status_id")
    private PaymentStatus previousStatus;

    @ManyToOne(optional = false, fetch = LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private PaymentStatus status;

    @Size(max = 256)
    @Column(name = "gateway_status", length = 256)
    private String gatewayStatus;

    @Column(nullable = false)
    private LocalDateTime timestamp;

}
