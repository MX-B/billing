package io.gr1d.billing.service.payment;

import io.gr1d.billing.exception.CardAuthorizationException;
import io.gr1d.billing.exception.ChargeException;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.request.CardAuthorizationRequest;

import java.time.LocalDate;

public interface PaymentStrategy {

    String charge(Invoice invoice, Card card) throws ChargeException;

    String authorizeCard(CardAuthorizationRequest request) throws CardAuthorizationException;

    LocalDate getPaymentDate(String transactionId) throws ChargeException;
}
