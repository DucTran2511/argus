package com.argus.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BlockchainException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public BlockchainException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR, "BLOCKCHAIN_ERROR");
    }

    public BlockchainException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorCode = "BLOCKCHAIN_ERROR";
    }

    public BlockchainException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
