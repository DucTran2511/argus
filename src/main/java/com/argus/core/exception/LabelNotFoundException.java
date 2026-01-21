package com.argus.core.exception;

import org.springframework.http.HttpStatus;

public class LabelNotFoundException extends DomainException {

    private static final String ERROR_CODE = "LABEL_NOT_FOUND";
    private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

    public LabelNotFoundException(String address) {
        super(ERROR_CODE, "No labels found for address: " + address);
    }

    public HttpStatus getHttpStatus() {
        return HTTP_STATUS;
    }
}
