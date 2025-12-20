package com.argus.core.exception;

public class BlockchainException extends DomainException {

    private static final String ERROR_CODE = "BLOCKCHAIN_ERROR";

    public BlockchainException(String message) {
        super(ERROR_CODE, message);
    }

    public BlockchainException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
