package io.gr1d.billing.service.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;

@Component
public class PaymentTestUtil {

    private final Boolean chargeDebug;
    private final ThreadLocal<PaymentConfig> config = new ThreadLocal<>();

    public PaymentTestUtil(@Value("${gr1d.billing.chargeDebug}") final Boolean chargeDebug) {
        this.chargeDebug = chargeDebug;
    }

    public void simulatePaymentError(final Boolean simulatePaymentError) {
        if (chargeDebug) {
            config.set(new PaymentConfig(ofNullable(simulatePaymentError).orElse(false)));
        }
    }

    public boolean isActive() {
        return chargeDebug;
    }

    public boolean isSimulatePaymentErrorActive() {
        return ofNullable(config.get()).map(PaymentConfig::getSimulatePaymentError).orElse(false);
    }

    @Getter
    @AllArgsConstructor
    public static class PaymentConfig {
        public final Boolean simulatePaymentError;
    }

}
