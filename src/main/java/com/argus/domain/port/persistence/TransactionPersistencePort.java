package com.argus.domain.port.persistence;

import com.argus.domain.model.Transaction;

public interface TransactionPersistencePort {
    Transaction save(Transaction transaction);
}
