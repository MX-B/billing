package io.gr1d.billing.exception;

import io.gr1d.core.exception.Gr1dHttpException;
import org.springframework.http.HttpStatus;

public class TransferLetterNotFoundException extends Gr1dHttpException {

    public TransferLetterNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "io.gr1d.billing.transferLetterNotFound");
    }

}
