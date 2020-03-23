package io.gr1d.billing.service.payment;

import lombok.extern.slf4j.Slf4j;
import me.pagar.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Service for center the communication with Pagar.me
 *
 * @author sergio.filho@moneyex.io
 */
@Slf4j
@Service
public class PagarmeService {

    private final PaymentTestUtil paymentTestUtil;

    @Autowired
    public PagarmeService(@Value("${gr1d.pagarme.apiKey}") final String apiKey, final PaymentTestUtil paymentTestUtil) {
        this.paymentTestUtil = paymentTestUtil;
        PagarMe.init(apiKey);
    }

    public Transaction save(final Transaction transaction) throws PagarMeException {
        if (paymentTestUtil.isActive() && paymentTestUtil.isSimulatePaymentErrorActive()) {
            transaction.setCardCvv("600");
        }

        return transaction.save();
    }

    public Card save(final Card card) throws PagarMeException {
        return card.save();
    }

    public boolean validateRequestSignature(final String payload, final String signature) {
        return PagarMe.validateRequestSignature(payload, signature);
    }

    public Collection<Payable> find(String transactionId) throws PagarMeException {
        return new Transaction().find(transactionId).payables();
    }

}
