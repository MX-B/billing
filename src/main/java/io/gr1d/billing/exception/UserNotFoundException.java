package io.gr1d.billing.exception;

import io.gr1d.core.exception.Gr1dHttpException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends Gr1dHttpException {

    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "io.gr1d.billing.userNotFound");
    }

}
