package io.gr1d.billing.exception;

import io.gr1d.core.exception.Gr1dHttpRuntimeException;
import org.springframework.http.HttpStatus;

public class CardNotFoundException extends Gr1dHttpRuntimeException {

    public CardNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "io.gr1d.billing.cardInfoNotFound");
    }

}
