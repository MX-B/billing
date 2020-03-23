package io.gr1d.billing.service.payment;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

public interface PaymentWebhookHandler {

    /**
     * Handle a webhook invocation by an external gateway.
     *
     * @param request
     *            The {@link HttpServletRequest} associated with the request
     * @param requestEntity
     *            A {@link HttpEntity} of the request (to get its raw data)
     * @return What should be sent back as a response to the Payment Gateway
     */
    <T> ResponseEntity<T> handle(final HttpServletRequest request, final HttpEntity<String> requestEntity);

}
