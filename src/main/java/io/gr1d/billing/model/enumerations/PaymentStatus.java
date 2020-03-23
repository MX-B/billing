package io.gr1d.billing.model.enumerations;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "payment_status")
public class PaymentStatus implements Serializable {

    public static final PaymentStatus CREATED = new PaymentStatus(1L, "CREATED", true);
    public static final PaymentStatus PROCESSING = new PaymentStatus(2L, "PROCESSING", false);
    public static final PaymentStatus PROCESSING_RETRY = new PaymentStatus(3L, "PROCESSING_RETRY", false);
    public static final PaymentStatus REFUNDING = new PaymentStatus(4L, "REFUNDING", false);
    public static final PaymentStatus REFUNDED = new PaymentStatus(5L, "REFUNDED", false);
    public static final PaymentStatus FAILED = new PaymentStatus(6L, "FAILED", true);
    public static final PaymentStatus SUCCESS = new PaymentStatus(7L, "SUCCESS", false);
    public static final PaymentStatus NO_CARD = new PaymentStatus(8L, "NO_CARD", true);
    public static final PaymentStatus FAILED_COMMUNICATION = new PaymentStatus(9L, "FAILED_COMMUNICATION", true);
    public static final PaymentStatus CANCELED = new PaymentStatus(10L, "CANCELED", false);
    public static final PaymentStatus SETTLED = new PaymentStatus(11L, "SETTLED", false);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private boolean chargeable;

    private PaymentStatus(final Long id, final String name, final boolean chargeable) {
        this.id = id;
        this.name = name;
        this.chargeable = chargeable;
    }

    public boolean equals(final Object object) {
        return object instanceof PaymentStatus && ((PaymentStatus) object).getName().equals(getName());
    }

    public int hashCode() {
        return id.hashCode();
    }

    public String toString() {
        return getName();
    }

}
