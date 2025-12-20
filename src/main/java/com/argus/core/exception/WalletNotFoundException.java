package com.argus.core.exception;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletNotFoundException extends DomainException {

    private static final Logger log = LoggerFactory.getLogger(WalletNotFoundException.class);
    private static final String ERROR_CODE = "WALLET_NOT_FOUND";
    private static final String USER_MESSAGE = "The referenced wallet does not exist";

    private final UUID walletId;

    public WalletNotFoundException(UUID walletId) {
        super(ERROR_CODE, USER_MESSAGE);
        this.walletId = walletId;
        log.error("Wallet not found in database. WalletId: {}", walletId);
    }

    public UUID getWalletId() {
        return walletId;
    }
}
