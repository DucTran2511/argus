package com.argus.domain.port.blockchain;

import com.argus.domain.model.Transaction;
import java.util.Optional;

public interface BlockChainPort {

    long getLatestBlockNumber();

    Optional<Transaction> getTransactionByHash(String txHash);
}
