package io.gr1d.billing.validation;

import io.gr1d.billing.api.subscriptions.Provider;
import io.gr1d.billing.service.ProviderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class ProviderExistsValidator implements ConstraintValidator<ProviderExists, String> {

    private final ProviderService providerService;

    @Autowired
    public ProviderExistsValidator(final ProviderService providerService) {
        this.providerService = providerService;
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (StringUtils.isEmpty(value)) {
            return true;
        }
        try {
            final Provider provider = providerService.getProviderData(value);
            return provider != null;
        } catch (final Exception e) {
            log.error("Error trying to validate if provider exists", e);
            return false;
        }
    }

}
