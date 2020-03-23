package io.gr1d.billing.controller;

import io.gr1d.auth.keycloak.LoggedUser;
import io.gr1d.billing.exception.PayableNotFoundException;
import io.gr1d.billing.exception.PayableNotProvidedException;
import io.gr1d.billing.exception.TransferLetterNotFoundException;
import io.gr1d.billing.exception.TransferLetterProviderNotFoundException;
import io.gr1d.billing.model.transfer.TransferLetter;
import io.gr1d.billing.request.transfer.TransferRequest;
import io.gr1d.billing.response.*;
import io.gr1d.billing.service.TransferLetterService;
import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.datasource.model.PageResult;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import net.kaczmarzyk.spring.data.jpa.domain.In;
import net.kaczmarzyk.spring.data.jpa.domain.NotNull;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Slf4j
@RestController
@Api(tags = "Transfer Letter", description = "Transfer letters are generated every month grouping all payable debts to providers")
@RequestMapping(path = "/transfer-letter")
public class TransferLetterController extends BaseController {

    private final TransferLetterService transferLetterService;
    private final LoggedUser loggedUser;

    @Autowired
    public TransferLetterController(final TransferLetterService transferLetterService, final LoggedUser loggedUser) {
        this.transferLetterService = transferLetterService;
        this.loggedUser = loggedUser;
    }

    @And({
            @Spec(path = "status.name", params = "status", spec = In.class),
            @Spec(path = "removedAt", params = "removed", spec = NotNull.class, constVal = "false")
    })
    private interface TransferLetterSpec extends Specification<TransferLetter> {
    }

    private Specification<TransferLetter> periodGreaterEqual(final LocalDate period) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("finishDate"), period);
    }

    private Specification<TransferLetter> periodLessEqual(final LocalDate period) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), period);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "status", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "period_ge", dataType = "date", paramType = "query", format = "yyyy-MM-dd"),
            @ApiImplicitParam(name = "period_le", dataType = "date", paramType = "query", format = "yyyy-MM-dd"),
    })
    @ApiOperation(nickname = "listTransferLetters", value = "List Transfer Letters", notes = "Returns a list with all Transfer Letters", tags = "Transfer Letter")
    @RequestMapping(path = "", method = GET, produces = JSON)
    public PageResult<TransferLetterResponse> list(final TransferLetterSpec specification, final Gr1dPageable page,
                                                   @ApiParam(hidden = true) final LocalDate periodGe,
                                                   @ApiParam(hidden = true) final LocalDate periodLe) {
        log.info("Listing TransferLetter {}", page);

        Specification<TransferLetter> spec = specification;
        if (periodGe != null) {
            spec = spec.and(periodGreaterEqual(periodGe));
        }
        if (periodLe != null) {
            spec = spec.and(periodLessEqual(periodLe));
        }

        return transferLetterService.list(spec, page.toPageable());
    }

    @ApiOperation(nickname = "getTransferLetter", value = "Get Transfer Letters", notes = "Returns a Transfer Letter", tags = "Transfer Letter")
    @RequestMapping(path = "/{uuid:[A-Za-z0-9\\-]+}", method = GET, produces = JSON)
    public TransferLetterResponse getTransferLetter(@PathVariable("uuid") final String uuid) throws TransferLetterNotFoundException {
        return transferLetterService.find(uuid);
    }

    @ApiOperation(nickname = "listPayables", value = "List payables from Transfer Letter", notes = "Returns a list of all payables from Transfer Letter for a given provider", tags = "Transfer Letter")
    @RequestMapping(path = "/{uuid:[A-Za-z0-9\\-]+}/provider/{providerUuid:[A-Za-z0-9\\-]+}", method = GET, produces = JSON)
    public List<PayableResponse> listByProvider(@PathVariable("uuid") final String uuid,
                                                @PathVariable("providerUuid") final String providerUuid) throws TransferLetterProviderNotFoundException {
        log.info("Listing all Payables by TransferLetter/Provider {}/{}", uuid, providerUuid);
        return transferLetterService.listByProvider(uuid, providerUuid);
    }

    @ApiOperation(nickname = "listProviders", value = "List providers from Transfer Letter", notes = "Returns a list of all providers from Transfer Letter for a given uuid", tags = "Transfer Letter")
    @RequestMapping(path = "/{uuid:[A-Za-z0-9\\-]+}/provider", method = GET, produces = JSON)
    public List<TransferLetterProviderResponse> listProviders(@PathVariable("uuid") final String uuid) {
        log.info("Listing all Providers by TransferLetter {}", uuid);
        return transferLetterService.listProviders(uuid);
    }

    @ApiOperation(nickname = "listTenants", value = "List tenants from Transfer Letter", notes = "Returns a list of all tenants from Transfer Letter for a given uuid", tags = "Transfer Letter")
    @RequestMapping(path = "/{uuid:[A-Za-z0-9\\-]+}/tenant", method = GET, produces = JSON)
    public List<TransferLetterTenantResponse> listTenants(@PathVariable("uuid") final String uuid) {
        log.info("Listing all Tenants by TransferLetter {}", uuid);
        return transferLetterService.listTenants(uuid);
    }

    @ApiOperation(nickname = "listPayableEndpoints", value = "List endpoints from Payable", notes = "Returns a list of all endpoints from Payable for a given uuid", tags = "Transfer Letter")
    @RequestMapping(path = "/payable/{uuid:[A-Za-z0-9\\-]+}/endpoints", method = GET, produces = JSON)
    public List<PayableEndpointResponse> listPayableEndpoints(@PathVariable("uuid") final String uuid) {
        log.info("Listing all Endpoints by Payable {}", uuid);
        return transferLetterService.listEndpoints(uuid);
    }

    @RequestMapping(path = "/{uuid:[A-Za-z0-9\\-]+}/transfer", method = POST, consumes = JSON)
    @ApiOperation(nickname = "transfer", value = "Transfer", notes = "Performs a transfer for a Provider", tags = "Transfer Letter")
    public TransferLetterProviderResponse transfer(@PathVariable("uuid") final String uuid, @Valid @RequestBody final TransferRequest request)
            throws PayableNotFoundException, TransferLetterNotFoundException,
            PayableNotProvidedException, TransferLetterProviderNotFoundException {
        log.info("Transfering from TransferLetter {} with request {}", uuid, request);
        return transferLetterService.transfer(loggedUser, uuid, request);
    }

    @ApiOperation(nickname = "createTransferLetterNow", value = "Create TransferLetter", notes = "Create Transfer Letter Now, only in chargeDebug", hidden = true)
    @RequestMapping(path = "/createTransferLetterNow", method = POST, consumes = JSON)
    public ResponseEntity<?> createTransferLetterNow(@Value("${gr1d.billing.chargeDebug}") final Boolean chargeDebug) {
        if (chargeDebug) {
            final LocalDate date = LocalDate.now();
            log.info("Request to create new TransferLetter now ({})", date);
            transferLetterService.createPayables(date);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
