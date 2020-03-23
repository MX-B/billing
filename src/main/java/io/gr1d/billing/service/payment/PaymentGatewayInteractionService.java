package io.gr1d.billing.service.payment;

import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.model.invoice.PaymentGatewayInteraction;
import io.gr1d.billing.repository.PaymentGatewayInteractionRepository;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class PaymentGatewayInteractionService {

    private final PaymentGatewayInteractionRepository repository;

    @Autowired
    public PaymentGatewayInteractionService(final PaymentGatewayInteractionRepository repository) {
        this.repository = repository;
    }

    @Builder
    public static class Interaction {
        private final String requestId;
        private final String endpoint;
        private final String method;
        private final String payload;
        private final int returnCode;

        private PaymentGatewayInteraction convert() {
            final PaymentGatewayInteraction interaction = new PaymentGatewayInteraction();
            interaction.setTimestamp(LocalDateTime.now());
            interaction.setEndpoint(endpoint);
            interaction.setMethod(method);
            interaction.setPayload(payload);
            interaction.setRequestId(requestId);
            interaction.setReturnCode(returnCode);
            return interaction;
        }
    }

    @Async
    public void transactionRequest(final Invoice invoice, final Interaction interaction) {
        final PaymentGatewayInteraction gatewayInteraction = interaction.convert();
        gatewayInteraction.setInvoice(invoice);
        gatewayInteraction.setOperation("TRANSACTION_REQUEST");
        repository.save(gatewayInteraction);
    }

    @Async
    public void transactionResponse(final Invoice invoice, final Interaction interaction) {
        final PaymentGatewayInteraction gatewayInteraction = interaction.convert();
        gatewayInteraction.setInvoice(invoice);
        gatewayInteraction.setOperation("TRANSACTION_RESPONSE");
        repository.save(gatewayInteraction);
    }

    @Async
    public void transactionFailure(final Invoice invoice, final Interaction interaction) {
        final PaymentGatewayInteraction gatewayInteraction = interaction.convert();
        gatewayInteraction.setOperation("TRANSACTION_FAILURE");
        gatewayInteraction.setInvoice(invoice);
        repository.save(gatewayInteraction);
    }

    @Async
    public void transactionWebhookResponse(final Invoice invoice, final Interaction interaction) {
        final PaymentGatewayInteraction gatewayInteraction = interaction.convert();
        gatewayInteraction.setInvoice(invoice);
        gatewayInteraction.setOperation("TRANSACTION_WEBHOOK_RESPONSE");
        repository.save(gatewayInteraction);
    }

}
