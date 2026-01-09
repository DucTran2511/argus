package com.argus.domain.port.blockchain;

import com.argus.domain.model.AssetTransfer;
import com.argus.domain.model.AssetTransfer.TransferCategory;
import com.argus.domain.model.Transaction;
import java.util.List;
import java.util.Optional;

public interface BlockChainPort {

    long getLatestBlockNumber();

    Optional<Transaction> getTransactionByHash(String txHash);

    /**
     * Fetches all transactions from a specific block.
     * 
     * @param blockNumber the block number to fetch transactions from
     * @return list of transactions in the block
     */
    List<Transaction> getTransactionsByBlock(long blockNumber);

    /**
     * Fetches asset transfers for a specific wallet address using Alchemy API.
     * 
     * @param address    Wallet address to fetch transfers for
     * @param categories Transfer categories to include (EXTERNAL, ERC20, etc.)
     * @param maxCount   Maximum number of transfers to return
     * @return List of asset transfers
     */
    List<AssetTransfer> getWalletTransactions(
            String address,
            List<TransferCategory> categories,
            int maxCount);
}
