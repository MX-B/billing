package io.gr1d.billing.controller;

import io.gr1d.billing.exception.UserNotFoundException;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.response.UserResponse;
import io.gr1d.billing.service.KeycloakUserService;
import io.gr1d.billing.service.UserService;
import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.datasource.model.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.domain.NotNull;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

import static java.util.Optional.ofNullable;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@RestController
@Api(tags = "User")
@RequestMapping(path = "/user")
public class UserController extends BaseController {

    private final UserService userService;
    private final KeycloakUserService keycloakUserService;

    @Autowired
    public UserController(final UserService userService, final KeycloakUserService keycloakUserService) {
        this.keycloakUserService = keycloakUserService;
        this.userService = userService;
    }

    @RequestMapping(path = "/tenant/{realm}/searchByKeycloak", method = GET, produces = JSON)
    public Collection<io.gr1d.spring.keycloak.model.User> search(@PathVariable final String realm, @RequestParam final String search) {
        log.info("Searching Keycloak Users with realm/search {}/{}", realm, search);
        return keycloakUserService.search(realm, search);
    }

    @And({
            @Spec(path = "user.tenantRealm", params = "tenant_realm", spec = Equal.class),
            @Spec(path = "user.keycloakId", params = "keycloak_id", spec = Equal.class),
            @Spec(path = "user.removedAt", params = "removed", spec = NotNull.class, constVal = "false"),
            @Spec(path = "removedAt", params = "removed", spec = NotNull.class, constVal = "false")
    })
    private interface CardSpec extends Specification<Card> {

    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "tenant_realm", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "keycloak_id", dataType = "string", paramType = "query"),
    })
    @RequestMapping(path = "", method = GET, produces = JSON)
    @ApiOperation(nickname = "listUsers", value = "Get User List", notes = "Returns a list with all registered users")
    public PageResult<UserResponse> list(final CardSpec spec, final Gr1dPageable page) {
        log.info("Listing Users {}", page);
        return userService.list(spec, page.toPageable());
    }

    @ApiOperation(nickname = "getUserData", value = "Get User Data", notes = "Returns information about user")
    @RequestMapping(path = "/tenant/{tenantRealm}/{keycloakId:[A-Za-z0-9\\-]+}", method = GET, produces = JSON)
    public UserResponse get(@PathVariable final String tenantRealm, @PathVariable final String keycloakId) throws UserNotFoundException {
        log.info("Requesting User by Tenant(realm:{}) and User(keycloakId:{})", tenantRealm, keycloakId);

        return ofNullable(userService.find(tenantRealm, keycloakId))
                .map(UserResponse::new)
                .orElseThrow(UserNotFoundException::new);
    }

}
