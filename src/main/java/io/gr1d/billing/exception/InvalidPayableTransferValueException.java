package io.gr1d.billing.exception;

import io.gr1d.core.exception.Gr1dHttpRuntimeException;
import org.springframework.http.HttpStatus;

public class InvalidPayableTransferValueException extends Gr1dHttpRuntimeException {

    public InvalidPayableTransferValueException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY.value(), "io.gr1d.billing.invalidPayableTransferValue");
    }

}
