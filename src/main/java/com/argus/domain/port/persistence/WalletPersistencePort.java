package com.argus.domain.port.persistence;

import com.argus.domain.model.Wallet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletPersistencePort {

    Wallet save(Wallet wallet);

    Optional<Wallet> findById(UUID id);

    Optional<Wallet> findByAddress(String address);

    List<Wallet> findAll();

    List<Wallet> findByChain(String chain);

    List<Wallet> findByType(Wallet.WalletType type);

    List<Wallet> findByChainAndType(String chain, Wallet.WalletType type);

    List<Wallet> findTopPerformers(double minWinRate);

    boolean existsByAddress(String address);

    void delete(UUID id);

    void deleteByAddress(String address);
}
