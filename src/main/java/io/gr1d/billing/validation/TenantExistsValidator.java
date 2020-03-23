package io.gr1d.billing.validation;

import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class TenantExistsValidator implements ConstraintValidator<TenantExists, String> {

    private final TenantService tenantService;

    @Autowired
    public TenantExistsValidator(final TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (StringUtils.isEmpty(value)) {
            return true;
        }

        try {
            final Tenant tenant = tenantService.getTenantDataByRealm(value);
            return tenant != null;
        } catch (final Exception e) {
            log.error("Error trying to validate if tenant exists", e);
            return false;
        }
    }

}
