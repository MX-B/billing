package io.gr1d.billing.controller;

import io.gr1d.auth.keycloak.LoggedUser;
import io.gr1d.billing.exception.ChargeException;
import io.gr1d.billing.exception.UserNotFoundException;
import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.request.invoice.InvoiceCancelRequest;
import io.gr1d.billing.request.invoice.InvoiceRequest;
import io.gr1d.billing.response.InvoiceListResponse;
import io.gr1d.billing.response.InvoiceResponse;
import io.gr1d.billing.service.InvoiceService;
import io.gr1d.billing.service.payment.PaymentTestUtil;
import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.datasource.model.PageResult;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.domain.In;
import net.kaczmarzyk.spring.data.jpa.domain.NotNull;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@Slf4j
@RestController
@Api(tags = "Invoice")
@RequestMapping(path = "/invoice")
public class InvoiceController extends BaseController {

    private static final String HEADER_SIMULATE_ERROR = "X-Billing-Simulate-Error";

    private final InvoiceService service;
    private final PaymentTestUtil paymentTestUtil;
    private final LoggedUser loggedUser;

    @Autowired
    public InvoiceController(final InvoiceService invoiceService, final PaymentTestUtil paymentTestUtil, final LoggedUser loggedUser) {
        this.service = invoiceService;
        this.paymentTestUtil = paymentTestUtil;
        this.loggedUser = loggedUser;
    }

    @RequestMapping(method = POST, produces = JSON, consumes = JSON)
    @ApiOperation(nickname = "createInvoice", value = "Create Invoice", notes = "Creates a new invoice")
    public InvoiceResponse create(@Valid @RequestBody final InvoiceRequest request,
                                  @RequestHeader(value = HEADER_SIMULATE_ERROR, defaultValue = "false", required = false) final Boolean simulateError)
            throws UserNotFoundException {
        log.info("Creating invoice for request: {}", request);
        paymentTestUtil.simulatePaymentError(simulateError);
        return service.create(request);
    }

    @RequestMapping(method = GET, path = "/{uuid:[A-Za-z0-9\\-]+}", produces = JSON)
    @ApiOperation(nickname = "getInvoiceData", value = "Get Invoice Data", notes = "Returns information about an invoice")
    public InvoiceResponse get(@PathVariable("uuid") final String uuid) {
        log.info("Request for Invoice data: {}", uuid);
        return service.getInvoice(uuid);
    }

    @And({
            @Spec(path = "number", params = "number", spec = Equal.class),
            @Spec(path = "tenantRealm", params = "tenant", spec = Equal.class),
            @Spec(path = "paymentStatus.name", params = "status", spec = In.class),
            @Spec(path = "user.keycloakId", params = "keycloak_id", spec = Equal.class),
            @Spec(path = "removedAt", params = "removed", spec = NotNull.class, constVal = "false")
    })
    private interface InvoiceSpec extends Specification<Invoice> {

    }

    private Specification<Invoice> periodGreaterEqual(final LocalDate period) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("periodEnd"), period);
    }

    private Specification<Invoice> periodLessEqual(final LocalDate period) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("periodStart"), period);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "number", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "tenant", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "status", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "period_ge", dataType = "date", paramType = "query", format = "yyyy-MM-dd"),
            @ApiImplicitParam(name = "period_le", dataType = "date", paramType = "query", format = "yyyy-MM-dd"),
            @ApiImplicitParam(name = "keycloak_id", dataType = "string", paramType = "query"),
    })
    @RequestMapping(method = GET, path = "", produces = JSON)
    @ApiOperation(nickname = "listInvoices", value = "List Invoices", notes = "Returns a list with all registered invoices")
    public PageResult<InvoiceListResponse> list(final InvoiceSpec specification, final Gr1dPageable page,
                                                @ApiParam(hidden = true) final LocalDate periodGe,
                                                @ApiParam(hidden = true) final LocalDate periodLe) {
        log.info("Request for Invoice List: {}", page);

        Specification<Invoice> spec = specification;
        if (periodGe != null) {
            spec = spec.and(periodGreaterEqual(periodGe));
        }
        if (periodLe != null) {
            spec = spec.and(periodLessEqual(periodLe));
        }

        return service.list(spec, page.toPageable());
    }

    @RequestMapping(method = PUT, path = "/{uuid:[A-Za-z0-9\\-]+}/charge", produces = JSON)
    @ApiOperation(nickname = "chargeInvoice", value = "Charge Invoice", notes = "Charges a CREATED or FAILED invoice")
    public InvoiceResponse charge(@PathVariable("uuid") final String uuid,
                                  @RequestHeader(value = HEADER_SIMULATE_ERROR, defaultValue = "false", required = false) final Boolean simulateError)
            throws ChargeException {
        log.info("Charging invoice {}", uuid);
        paymentTestUtil.simulatePaymentError(simulateError);
        return service.charge(uuid);
    }

    @ApiOperation(nickname = "cancelInvoice", value = "Cancel Invoice", notes = "Cancel an existing Invoice with status CREATED")
    @RequestMapping(method = PUT, path = "/{uuid:[A-Za-z0-9\\-]+}/cancel", consumes = JSON)
    public void cancel(@PathVariable final String uuid, @RequestBody @Valid final InvoiceCancelRequest request) {
        log.info("Request for invoice cancel: {}", uuid);
        service.cancel(loggedUser, uuid, request);
    }

}
