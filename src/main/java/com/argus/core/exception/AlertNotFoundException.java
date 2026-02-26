package com.argus.core.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AlertNotFoundException extends DomainException {

    private static final Logger log = LoggerFactory.getLogger(AlertNotFoundException.class);
    private static final String ERROR_CODE = "ALERT_NOT_FOUND";
    private static final String USER_MESSAGE = "The referenced alert does not exist";
    private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

    public AlertNotFoundException(UUID id) {
        super(ERROR_CODE, USER_MESSAGE);
        log.warn("Alert not found: {}", id);
    }

    public HttpStatus getHttpStatus() {
        return HTTP_STATUS;
    }

}
