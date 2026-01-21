package com.argus.core.exception;

import org.springframework.http.HttpStatus;

public class LabelAlreadyExistsException extends DomainException {

    private static final String ERROR_CODE = "LABEL_ALREADY_EXISTS";
    private static final HttpStatus HTTP_STATUS = HttpStatus.CONFLICT;

    public LabelAlreadyExistsException(String address, String label) {
        super(ERROR_CODE, "Label '" + label + "' already exists for address: " + address);
    }

    public HttpStatus getHttpStatus() {
        return HTTP_STATUS;
    }
}
