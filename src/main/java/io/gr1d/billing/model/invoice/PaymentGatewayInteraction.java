package io.gr1d.billing.model.invoice;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payment_gateway_interaction")
public class PaymentGatewayInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    private String operation;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "request_id")
    private String requestId;
    private String endpoint;
    private String method;
    private String payload;

    @Column(name = "return_code")
    private int returnCode;
}
