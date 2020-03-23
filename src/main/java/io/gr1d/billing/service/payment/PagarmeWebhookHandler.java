package io.gr1d.billing.service.payment;

import io.gr1d.billing.exception.ChargeException;
import io.gr1d.billing.model.enumerations.PaymentStatus;
import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.repository.InvoiceRepository;
import io.gr1d.billing.service.InvoiceStatusService;
import io.gr1d.billing.service.payment.PaymentGatewayInteractionService.Interaction;
import io.gr1d.core.util.Markers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.*;

import static java.util.Optional.ofNullable;

@Slf4j
@Component("PaymentWebhookHandler.PAGARME")
public class PagarmeWebhookHandler implements PaymentWebhookHandler {

    /**
     * A map from Pagar.me statuses to our own
     */
    private static final Map<String, PaymentStatus> STATUSES_MAP;

    static {
        final Map<String, PaymentStatus> map = new HashMap<>(8);
        map.put("processing", PaymentStatus.PROCESSING);
        map.put("authorized", PaymentStatus.PROCESSING);
        map.put("paid", PaymentStatus.SUCCESS);
        map.put("refunded", PaymentStatus.REFUNDED);
        map.put("waiting_payment", PaymentStatus.PROCESSING);
        map.put("pending_refund", PaymentStatus.REFUNDING);
        map.put("refused", PaymentStatus.FAILED);
        map.put("chargedback", PaymentStatus.REFUNDED);

        STATUSES_MAP = Collections.unmodifiableMap(map);
    }

    public static final String SIGNATURE_HEADER = "X-Hub-Signature";
    private static final String STATUS = "current_status";
    private static final String INVOICE_ID = "transaction[metadata][" + PagarmePaymentStrategy.INVOICE_ID_METADATA + "]";
    private static final String REQUEST_ID = "transaction[metadata][" + PagarmePaymentStrategy.REQUEST_ID_METADATA + "]";
    private static final String POSTBACK_TYPE = "object";
    private static final String POSTBACK_TYPE_TRANSACTION = "transaction";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceStatusService invoiceStatusService;
    private final PaymentGatewayInteractionService interactionService;
    private final PagarmePaymentStrategy paymentStrategy;

    private PagarmeService pagarmeService;

    @Autowired
    public PagarmeWebhookHandler(final InvoiceRepository invoiceRepository,
                                 final InvoiceStatusService invoiceStatusService,
                                 final PaymentGatewayInteractionService interactionService,
                                 final PagarmePaymentStrategy paymentStrategy) {

        this.invoiceRepository = invoiceRepository;
        this.invoiceStatusService = invoiceStatusService;
        this.interactionService = interactionService;
        this.paymentStrategy = paymentStrategy;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public <T> ResponseEntity<T> handle(final HttpServletRequest request, final HttpEntity<String> requestEntity) {
        log.debug("Processing Pagar.me Webhook Call: {}", request);

        final String requestSignature = request.getHeader(SIGNATURE_HEADER);
        final String requestPayload = ofNullable(requestEntity.getBody())
                .map(s -> s.replaceAll("\\+", "%20"))
                .orElse("");
        final boolean validRequest = request.getMethod().equals("POST") && pagarmeService.validateRequestSignature(requestPayload, requestSignature);

        log.debug("Valid Pagar.me request? {} - signature={}, payload={}", validRequest, requestSignature, requestPayload);

        if (validRequest) {
            final String postbackType = request.getParameter(POSTBACK_TYPE);

            if (!StringUtils.isEmpty(postbackType) && postbackType.equals(POSTBACK_TYPE_TRANSACTION)) {
                final String invoiceId = request.getParameter(INVOICE_ID);
                final String requestId = request.getParameter(REQUEST_ID);
                final Optional<Invoice> invoice = invoiceRepository.findByUuid(invoiceId);

                final Interaction interaction = Interaction.builder()
                        .payload(requestPayload)
                        .requestId(requestId)
                        .build();
                interactionService.transactionWebhookResponse(invoice.orElse(null), interaction);

                if (invoice.isPresent()) {
                    final String gatewayStatus = request.getParameter(STATUS);
                    final PaymentStatus status = asPaymentStatus(gatewayStatus);
                    final Invoice theInvoice = invoice.get();

                    if (PaymentStatus.SUCCESS.equals(status)) {
                        try {
                            final LocalDate paymentDate = paymentStrategy.getPaymentDate(theInvoice.getGatewayTransactionId());
                            theInvoice.setSettlementDate(paymentDate);
                        } catch (ChargeException e) {
                            log.error(Markers.NOTIFY_ADMIN, "Error while trying to retrieve payment date from invoice {}", theInvoice.getUuid(), e);
                        }
                    }

                    invoiceStatusService.updateStatus(theInvoice, status, gatewayStatus);

                    return ResponseEntity.ok().build();
                } else {
                    log.error("Invoice not found: {}", invoiceId);
                }
            } else {
                log.error("Invalid Pagar.me payload type: {} (expected: {})", postbackType, POSTBACK_TYPE_TRANSACTION);
            }
        } else {
            log.error("Invalid Pagar.me request! - signature={}, payload={}, headers={}", requestSignature, requestPayload, headers(request));
        }

        return ResponseEntity.badRequest().build();
    }

    /**
     * @return A corresponding {@link PaymentStatus} from Pagar.me's statuses
     * @see "https://docs.pagar.me/reference#status-das-transacoes"
     */
    private PaymentStatus asPaymentStatus(final String status) {
        return ofNullable(status)
                .map(STATUSES_MAP::get)
                .orElse(null);
    }

    /**
     * @return {@link HttpServletRequest#getHeader(String) Headers} as a {@link Map}
     */
    private Map<String, String> headers(final HttpServletRequest request) {
        final Enumeration<String> headerNames = request.getHeaderNames();
        final Map<String, String> headers = new HashMap<>();

        while (headerNames.hasMoreElements()) {
            final String header = headerNames.nextElement();
            headers.put(header, request.getHeader(header));
        }

        return headers;
    }

    @Autowired
    public void setPagarmeService(final PagarmeService pagarmeService) {
        this.pagarmeService = pagarmeService;
    }
}
