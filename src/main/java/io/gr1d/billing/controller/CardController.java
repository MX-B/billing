package io.gr1d.billing.controller;

import io.gr1d.billing.exception.CardAuthorizationException;
import io.gr1d.billing.request.AddressRequest;
import io.gr1d.billing.request.CardAuthorizationRequest;
import io.gr1d.billing.response.CardResponse;
import io.gr1d.billing.service.CardService;
import io.gr1d.core.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@Slf4j
@RestController
@Api(tags = "Card")
@RequestMapping(path = "/card")
public class CardController extends BaseController {

    private final CardService cardService;

    public CardController(final CardService cardService) {
        this.cardService = cardService;
    }

    @RequestMapping(method = POST, path = "/authorize", consumes = JSON, produces = JSON)
    @ApiOperation(nickname = "authorizeCard", value = "Authorize Card", notes = "Authorizes a new card")
    @ApiResponse(code = 420, message = "Unauthorized Card")
    public CardResponse authorize(@RequestBody @Valid final CardAuthorizationRequest request) throws CardAuthorizationException {
        log.info("Authorizing Card with request {}", request);
        return cardService.authorizeCard(request);
    }

    @ApiOperation(nickname = "getCardInfo", value = "Get Card Data", notes = "Returns information about a registered card")
    @RequestMapping(method = GET, path = "/tenant/{tenantRealm}/user/{userId}", produces = JSON)
    public CardResponse getCardInfo(@PathVariable final String tenantRealm, @PathVariable final String userId) {
        log.info("Requesting CardInfo Tenant(realm:{}) User(keycloakId:{})", tenantRealm, userId);
        return cardService.getForUser(tenantRealm, userId);
    }

    @ApiOperation(nickname = "updateAddressInfo", value = "Update Address", notes = "Updates the address of a card")
    @RequestMapping(method = POST, path = "/tenant/{tenantRealm}/user/{userId}/address", produces = JSON)
    public CardResponse updateAddressInfo(@PathVariable final String tenantRealm,
                                          @PathVariable final String userId,
                                          @RequestBody @Valid final AddressRequest request) {
        log.info("Updating Card Address Tenant(realm:{}) User(keycloakId:{}) with request {}", tenantRealm, userId, request);
        return cardService.updateAddressInfo(tenantRealm, userId, request);
    }
}
