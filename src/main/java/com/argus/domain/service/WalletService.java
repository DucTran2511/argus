package com.argus.domain.service;

import com.argus.core.exception.WalletNotFoundException;
import com.argus.domain.model.Wallet;
import com.argus.domain.port.persistence.WalletPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletPersistencePort walletPersistencePort;

    public Wallet createWallet(Wallet wallet) {
        log.info("Creating wallet with address: {}", wallet.getAddress());

        Wallet walletToCreate = Wallet.builder()
                .address(wallet.getAddress())
                .chain(wallet.getChain())
                .label(wallet.getLabel())
                .type(wallet.getType())
                .totalPnl(wallet.getTotalPnl())
                .winRate(wallet.getWinRate())
                .firstSeenAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return walletPersistencePort.save(walletToCreate);
    }

    public Wallet getWalletById(UUID id) {
        log.debug("Fetching wallet by id: {}", id);
        return walletPersistencePort.findById(id)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + id));
    }

    public Wallet getWalletByAddress(String address) {
        log.debug("Fetching wallet by address: {}", address);
        return walletPersistencePort.findByAddress(address)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with address: " + address));
    }

    public List<Wallet> getAllWallets() {
        log.debug("Fetching all wallets");
        return walletPersistencePort.findAll();
    }

    public List<Wallet> getWalletsByType(Wallet.WalletType type) {
        log.debug("Fetching wallets by type: {}", type);
        return walletPersistencePort.findByType(type);
    }

    public Wallet updateWallet(UUID id, Wallet updatedWallet) {
        log.info("Updating wallet with id: {}", id);

        Wallet existingWallet = getWalletById(id);

        Wallet walletToUpdate = Wallet.builder()
                .id(existingWallet.getId())
                .address(existingWallet.getAddress())
                .chain(existingWallet.getChain())
                .label(updatedWallet.getLabel() != null ? updatedWallet.getLabel() : existingWallet.getLabel())
                .type(updatedWallet.getType() != null ? updatedWallet.getType() : existingWallet.getType())
                .totalPnl(updatedWallet.getTotalPnl() != null ? updatedWallet.getTotalPnl()
                        : existingWallet.getTotalPnl())
                .winRate(updatedWallet.getWinRate() != null ? updatedWallet.getWinRate() : existingWallet.getWinRate())
                .firstSeenAt(existingWallet.getFirstSeenAt())
                .lastActivityAt(existingWallet.getLastActivityAt())
                .createdAt(existingWallet.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        return walletPersistencePort.save(walletToUpdate);
    }

    public void deleteWallet(UUID id) {
        log.info("Deleting wallet with id: {}", id);

        if (!walletPersistencePort.existsById(id)) {
            throw new WalletNotFoundException("Wallet not found with id: " + id);
        }

        walletPersistencePort.deleteById(id);
    }

    public boolean walletExists(String address) {
        return walletPersistencePort.findByAddress(address).isPresent();
    }
}
