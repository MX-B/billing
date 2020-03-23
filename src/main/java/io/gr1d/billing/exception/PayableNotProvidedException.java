package io.gr1d.billing.exception;

import io.gr1d.core.exception.Gr1dHttpException;
import org.springframework.http.HttpStatus;

public class PayableNotProvidedException extends Gr1dHttpException {

    public PayableNotProvidedException(final String uuid) {
        super(HttpStatus.UNPROCESSABLE_ENTITY.value(), "io.gr1d.billing.payableNotProvided", new Object[] { uuid });
    }

}
