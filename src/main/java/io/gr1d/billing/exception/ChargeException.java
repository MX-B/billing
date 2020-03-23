package io.gr1d.billing.exception;

import io.gr1d.core.exception.Gr1dHttpException;
import org.springframework.http.HttpStatus;

public class ChargeException extends Gr1dHttpException {

    public ChargeException(final String message) {
        this(message, new Object[] {});
    }

    public ChargeException(final String message, final Object [] args) {
        super(HttpStatus.UNPROCESSABLE_ENTITY.value(), message, args);
    }

}
