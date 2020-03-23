package io.gr1d.billing.service.payment;

import io.gr1d.billing.exception.CardAuthorizationException;
import io.gr1d.billing.exception.ChargeException;
import io.gr1d.billing.model.DocumentType;
import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.model.invoice.InvoiceItem;
import io.gr1d.billing.request.CardAuthorizationRequest;
import io.gr1d.billing.service.payment.PaymentGatewayInteractionService.Interaction;
import io.gr1d.billing.util.ToString;
import lombok.extern.slf4j.Slf4j;
import me.pagar.model.*;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component("PaymentStrategy.PAGARME")
public class PagarmePaymentStrategy implements PaymentStrategy {

    public static final String INVOICE_ID_METADATA = "invoice_uuid";
    static final String REQUEST_ID_METADATA = "request_id";

    private static final BigDecimal ONE_HUNDED = BigDecimal.valueOf(100);

    private final PaymentGatewayInteractionService interactionService;

    private PagarmeService pagarmeService;

    /** Our public endpoint (something like {@code https://billing-api.gr1d.io/}) */
    private final String systemEndpoint;

    @Autowired
    public PagarmePaymentStrategy(@Value("${gr1d.externalEndpoint}") final String systemEndpoint,
                                  final PaymentGatewayInteractionService interactionService) {
        this.systemEndpoint = systemEndpoint;
        this.interactionService = interactionService;
    }

    public String authorizeCard(CardAuthorizationRequest request) throws CardAuthorizationException {
        try {
            final Card pagarmeCard = new Card();
            pagarmeCard.setHolderName(request.getCardHolderName());
            pagarmeCard.setNumber(request.getCardNumber());
            pagarmeCard.setExpiresAt(request.getCardExpirationDate());
            pagarmeCard.setCvv(Integer.parseInt(request.getCardCvv()));
            final Card newCard = pagarmeService.save(pagarmeCard);

            if (!newCard.getValid()) {
                throw new CardAuthorizationException();
            }

            return newCard.getId();
        } catch (PagarMeException e) {
            log.error("Error while trying to authorize card", e);
            throw new CardAuthorizationException();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String charge(final Invoice invoice, final io.gr1d.billing.model.Card card) throws ChargeException {
        log.info("Using PagarMe to charge invoice - invoice={}, card={}", invoice, card);

        final BigDecimal value = invoice.getValue();
        final String requestId = UUID.randomUUID().toString();
        final Map<String, Object> metadata = new HashMap<>(1);

        metadata.put(INVOICE_ID_METADATA, invoice.getUuid());
        metadata.put(REQUEST_ID_METADATA, requestId);

        final Transaction transaction = new Transaction();
        transaction.setPaymentMethod(Transaction.PaymentMethod.CREDIT_CARD);
        transaction.setCardId(card.getCardId());
        transaction.setAmount(value.multiply(ONE_HUNDED).intValue());
        transaction.setAsync(true);
        transaction.setMetadata(metadata);
        transaction.setPostbackUrl(systemEndpoint + "/webhook/handle/pagarme");
        transaction.setCustomer(asCustomer(card));

        final Collection<Item> pagarmeItems = invoice.getItems().stream()
                .map(this::asItem)
                .collect(Collectors.toList());
        transaction.setItems(pagarmeItems);

        final Billing billing = new Billing();
        billing.setName(card.getFullName());
        billing.setAddress(asAddress(card));
        transaction.setBilling(billing);

        final Interaction requestInteraction = Interaction.builder()
                .requestId(requestId)
                .method("POST")
                .payload(transaction.toString())
                .endpoint("https://api.pagar.me/1/transactions").build();
        interactionService.transactionRequest(invoice, requestInteraction);

        try {
            final Transaction result = pagarmeService.save(transaction);
            final Interaction responseInteraction = Interaction.builder()
                    .requestId(requestId)
                    .method("POST")
                    .payload(result.toString())
                    .endpoint("https://api.pagar.me/1/transactions").build();
            interactionService.transactionResponse(invoice, responseInteraction);

            return String.valueOf(result.getId());
        } catch (final PagarMeException e) {
            final Interaction interaction = Interaction.builder()
                    .requestId(requestId)
                    .method("POST")
                    .payload(ToString.toString(e.getErrors()))
                    .endpoint("https://api.pagar.me/1/transactions").build();
            interactionService.transactionFailure(invoice, interaction);

            log.error("Error creating Pagarme Transaction", e);
            throw new ChargeException("io.gr1d.billing.chargeException");
        }
    }

    @Override
    public LocalDate getPaymentDate(final String transactionId) throws ChargeException {
        try {
            return pagarmeService.find(transactionId)
                    .stream()
                    .map(Payable::getPaymentDate)
                    .max(DateTime::compareTo)
                    .map(this::toLocalDate)
                    .orElse(null);
        } catch (PagarMeException e) {
            log.error("Error creating Pagarme Transaction", e);
            throw new ChargeException("io.gr1d.billing.chargeException");
        }
    }

    private LocalDate toLocalDate(final DateTime dateTime) {
        return LocalDate.of(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
    }

    private Customer asCustomer(final io.gr1d.billing.model.Card card) {
        final boolean isPerson = card.getDocumentType() == DocumentType.CPF;

        final Customer customer = new Customer();
        customer.setType(isPerson ? Customer.Type.INDIVIDUAL : Customer.Type.CORPORATION);
        customer.setExternalId(card.getUuid());
        customer.setName(card.getFullName());
        customer.setBirthday(card.getBirthDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        customer.setEmail(card.getUser().getEmail());
        customer.setCountry(card.getNationality());

        final Collection<Document> documents = new ArrayList<>(1);
        final Document document = new Document();
        document.setType(isPerson ? Document.Type.CPF : Document.Type.CNPJ);
        document.setNumber(card.getDocument());
        documents.add(document);
        customer.setDocuments(documents);

        final Collection<String> phones = new ArrayList<>(1);
        phones.add(card.getPhone());
        customer.setPhoneNumbers(phones);

        return customer;
    }

    private Address asAddress(final io.gr1d.billing.model.Card card) {
        final Address address = new Address();
        address.setCity(card.getAddress().getCity());
        address.setCountry(card.getAddress().getCountry());
        address.setState(card.getAddress().getState());
        address.setNeighborhood(card.getAddress().getNeighborhood());
        address.setStreet(card.getAddress().getStreet());
        address.setZipcode(card.getAddress().getZipcode());
        address.setStreetNumber(card.getAddress().getStreetNumber());
        address.setComplementary(card.getAddress().getComplementary());

        return address;
    }


    private Item asItem(final InvoiceItem chargeItem) {
        final Item item = new Item();
        item.setId(chargeItem.getExternalId());
        item.setTitle(chargeItem.getDescription());
        item.setUnitPrice(toValue(chargeItem.getUnitValue(), chargeItem.getQuantity()));
        item.setQuantity(1);
        item.setTangible(false);

        return item;
    }

    public static Integer toValue(final BigDecimal value, final Integer quantity) {
        final BigDecimal qtt = BigDecimal.valueOf(quantity);
        return value.multiply(ONE_HUNDED).multiply(qtt).intValue();
    }

    @Autowired
    public void setPagarmeService(final PagarmeService pagarmeService) {
        this.pagarmeService = pagarmeService;
    }

}
