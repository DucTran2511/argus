package com.argus.core.exception;

import org.springframework.http.HttpStatus;

public class MaxLabelsExceededException extends DomainException {

    private static final String ERROR_CODE = "MAX_LABELS_EXCEEDED";
    private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

    public MaxLabelsExceededException(String address, int maxLabels) {
        super(ERROR_CODE, "Maximum " + maxLabels + " labels per address exceeded for: " + address);
    }

    public HttpStatus getHttpStatus() {
        return HTTP_STATUS;
    }
}
