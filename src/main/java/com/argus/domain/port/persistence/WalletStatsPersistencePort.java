package com.argus.domain.port.persistence;

import com.argus.domain.model.WalletStats;
import java.util.List;
import java.util.Optional;

public interface WalletStatsPersistencePort {

    WalletStats save(WalletStats stats);

    List<WalletStats> saveAll(List<WalletStats> statsList);

    List<WalletStats> findByWalletAddress(String walletAddress);

    Optional<WalletStats> findByWalletAndToken(String walletAddress, String tokenAddress);

    long countByWallet(String walletAddress);

    long countProfitableByWallet(String walletAddress);

    List<String> findActiveWalletAddresses(int page, int size);
}