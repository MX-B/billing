package io.gr1d.billing.exception;

import io.gr1d.core.exception.Gr1dHttpRuntimeException;
import org.springframework.http.HttpStatus;

public class PayableAlreadyPaidException extends Gr1dHttpRuntimeException {

    public PayableAlreadyPaidException(final String uuid) {
        super(HttpStatus.UNPROCESSABLE_ENTITY.value(), "io.gr1d.billing.payableAlreadyPaid", new Object[] { uuid });
    }

}
