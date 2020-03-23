package io.gr1d.billing.controller;

import io.gr1d.billing.service.payment.PaymentWebhookHandler;
import io.gr1d.core.controller.BaseController;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

@Slf4j
@RestController
public class WebhookController extends BaseController {

    private final PaymentWebhookHandler webhookHandler;

    @Autowired
    public WebhookController(final PaymentWebhookHandler webhookHandler) {
        this.webhookHandler = webhookHandler;
    }

    @ApiOperation(nickname = "webhook", value = "Pagar.me Webhook", notes = "Endpoint used by Pagar.me to postback")
    @RequestMapping(path = "/webhook/handle/pagarme", consumes = { JSON, XML, APPLICATION_FORM_URLENCODED })
    public ResponseEntity<?> handle(final HttpServletRequest request, final HttpEntity<String> requestEntity) {
        log.info("Handling Pagarme Webhook with Request = {}", request);

        final ResponseEntity<?> response = webhookHandler.handle(request, requestEntity);
        log.info("Handler response: {}", response);

        return response;
    }
}
