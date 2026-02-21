package com.argus.domain.service;

import com.argus.core.exception.WalletNotFoundException;
import com.argus.domain.model.Wallet;
import com.argus.domain.port.cache.BlockTrackingPort;
import com.argus.domain.port.persistence.WalletPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletPersistencePort walletPersistencePort;
    private final TransactionService transactionService;
    private final BlockTrackingPort blockTrackingPort;

    public Wallet createWallet(Wallet wallet, UUID userId) {
        log.info("Creating wallet with address: {} for user: {}", wallet.getAddress(), userId);

        Wallet walletToCreate = Wallet.builder()
                .userId(userId)
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

        Wallet saved = walletPersistencePort.save(walletToCreate);

        Thread.startVirtualThread(() -> {
            try {
                transactionService.syncWalletHistory(saved.getAddress(), 100);
                log.info("Synced history for wallet: {}", saved.getAddress());
            } catch (Exception e) {
                log.warn("Sync failed for {}: {}", saved.getAddress(), e.getMessage());
            }
        });

        blockTrackingPort.invalidateWalletCache();
        return saved;
    }

    public Wallet getWalletById(UUID id, UUID userId) {
        log.debug("Fetching wallet by id: {} for user: {}", id, userId);
        return walletPersistencePort.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + id));
    }

    public Wallet getWalletByAddress(String address, UUID userId) {
        log.debug("Fetching wallet by address: {} for user: {}", address, userId);
        return walletPersistencePort.findByAddressAndUserId(address, userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with address: " + address));
    }

    public List<Wallet> getAllWallets(UUID userId) {
        log.debug("Fetching all wallets for user: {}", userId);
        return walletPersistencePort.findByUserId(userId);
    }

    public List<Wallet> getWalletsByType(Wallet.WalletType type, UUID userId) {
        log.debug("Fetching wallets by type: {} for user: {}", type, userId);
        // Note: Currently persistence port doesn't have a scoped type finder,
        // I'll filter manually for now or add it to port if needed.
        // Let's just filter for now.
        return walletPersistencePort.findByUserId(userId).stream()
                .filter(w -> w.getType() == type)
                .collect(Collectors.toList());
    }

    public Wallet updateWallet(UUID id, Wallet updatedWallet, UUID userId) {
        log.info("Updating wallet with id: {} for user: {}", id, userId);

        Wallet existingWallet = getWalletById(id, userId);

        Wallet walletToUpdate = Wallet.builder()
                .id(existingWallet.getId())
                .userId(existingWallet.getUserId())
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

    public void deleteWallet(UUID id, UUID userId) {
        log.info("Deleting wallet with id: {} for user: {}", id, userId);

        if (!walletPersistencePort.existsById(id)) {
            throw new WalletNotFoundException("Wallet not found with id: " + id);
        }

        // Check ownership
        walletPersistencePort.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found or not owned by user"));

        walletPersistencePort.deleteById(id);
        blockTrackingPort.invalidateWalletCache();
        log.info("Deleted wallet and invalidated cache");
    }

    public boolean walletExists(String address) {
        log.debug("Checking if wallet exists globally: {}", address);
        return walletPersistencePort.findByAddress(address).isPresent();
    }

    public boolean walletExistsForUser(String address, UUID userId) {
        log.debug("Checking if wallet exists for user {}: {}", userId, address);
        return walletPersistencePort.findByAddressAndUserId(address, userId).isPresent();
    }
}
