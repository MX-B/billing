package io.gr1d.billing.exception;

import io.gr1d.core.exception.Gr1dHttpRuntimeException;
import org.springframework.http.HttpStatus;

public class InvoiceNotFoundException extends Gr1dHttpRuntimeException {

    public InvoiceNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "io.gr1d.billing.invoiceNotFound");
    }

}
