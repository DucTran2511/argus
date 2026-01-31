package com.argus.core.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WalletNotFoundException extends DomainException {

    private static final Logger log = LoggerFactory.getLogger(WalletNotFoundException.class);
    private static final String ERROR_CODE = "WALLET_NOT_FOUND";
    private static final String USER_MESSAGE = "The referenced wallet does not exist";
    private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

    private final UUID walletId;

    public WalletNotFoundException(UUID walletId) {
        super(ERROR_CODE, USER_MESSAGE);
        this.walletId = walletId;
        log.error("Wallet not found in database. WalletId: {}", walletId);
    }

    public WalletNotFoundException(String message) {
        super(ERROR_CODE, message);
        this.walletId = null;
        log.error("Wallet not found: {}", message);
    }

    public UUID getWalletId() {
        return walletId;
    }

    public HttpStatus getHttpStatus() {
        return HTTP_STATUS;
    }
}
