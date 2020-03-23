package io.gr1d.billing.exception;

import io.gr1d.core.exception.Gr1dHttpException;
import org.springframework.http.HttpStatus;

public class CardAuthorizationException extends Gr1dHttpException {

    public CardAuthorizationException() {
        super(HttpStatus.METHOD_FAILURE.value(), "io.gr1d.billing.cardAuthorizationFailed");
    }

}
