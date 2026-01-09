package com.argus.core.exception;

import org.springframework.http.HttpStatus;

public class TransactionNotFoundException extends BlockchainException {

    public TransactionNotFoundException(String txHash) {
        super("Transaction not found: " + txHash, HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND");
    }
}
