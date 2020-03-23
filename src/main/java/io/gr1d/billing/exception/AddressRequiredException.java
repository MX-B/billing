package io.gr1d.billing.exception;

import io.gr1d.core.exception.Gr1dConstraintException;

public class AddressRequiredException extends Gr1dConstraintException {

    public AddressRequiredException() {
        super("address", "io.gr1d.billing.addressRequired");
    }
}
