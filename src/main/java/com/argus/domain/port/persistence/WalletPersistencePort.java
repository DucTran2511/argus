package com.argus.domain.port.persistence;

import com.argus.domain.model.Wallet;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WalletPersistencePort {

    Wallet save(Wallet wallet);

    Optional<Wallet> findById(UUID id);

    Optional<Wallet> findByAddress(String address);

    List<Wallet> findByUserId(UUID userId);

    Optional<Wallet> findByIdAndUserId(UUID id, UUID userId);

    Optional<Wallet> findByAddressAndUserId(String address, UUID userId);

    List<Wallet> findAll();

    List<Wallet> findByChain(String chain);

    List<Wallet> findByType(Wallet.WalletType type);

    List<Wallet> findByChainAndType(String chain, Wallet.WalletType type);

    List<Wallet> findTopPerformers(double minWinRate);

    boolean existsByAddress(String address);

    boolean existsById(UUID id);

    void delete(UUID id);

    void deleteById(UUID id);

    void deleteByAddress(String address);

    Set<String> getAllAddresses();
}
