package io.gr1d.billing.controller;

import io.gr1d.billing.model.Currency;
import io.gr1d.billing.repository.CurrencyRepository;
import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.datasource.model.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.domain.Like;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@RestController
@Api(tags = "Currency")
@RequestMapping(path = "/currency")
public class CurrencyController extends BaseController {

    private final CurrencyRepository currencyRepository;

    public CurrencyController(final CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @And({
            @Spec(path = "name", params = "name", spec = Like.class),
            @Spec(path = "isoCode", params = "iso_code", spec = Equal.class)
    })
    private interface CurrencySpec extends Specification<Currency> {
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "iso_code", dataType = "string", paramType = "query")
    })
    @ApiOperation(nickname = "listCurrencies", value = "List Currencies", notes = "Returns a list with all registered Currencies", tags = "Currency")
    @RequestMapping(path = "", method = GET, produces = JSON)
    public PageResult<Currency> list(final CurrencySpec specification, final Gr1dPageable page) {
        log.info("Listing Currencies {}", page);
        return PageResult.ofPage(currencyRepository.findAll(specification, page.toPageable()));
    }
}
