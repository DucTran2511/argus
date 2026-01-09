package com.argus.domain.port.persistence;

import java.time.LocalDateTime;
import java.util.List;

import com.argus.domain.model.AssetTransfer;
import com.argus.domain.model.Transaction;

public interface TransactionPersistencePort {
    Transaction save(Transaction transaction);

    // Bulk save asset transfers (handles duplicates)
    List<AssetTransfer> saveAssetTransfers(List<AssetTransfer> transfers);

    // Find transfers by wallet address
    List<AssetTransfer> findByWalletAddress(String address, int limit, String order);

    // Find transfers within date range
    List<AssetTransfer> findByWalletAddressAndDateRange(
            String address,
            LocalDateTime from,
            LocalDateTime to);

    // Count total transfers for a wallet
    long countByWalletAddress(String address);
}
