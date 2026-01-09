package com.argus.api;

import com.argus.api.dto.WalletRequest;
import com.argus.api.dto.WalletResponse;
import com.argus.api.dto.response.AssetTransferResponse;
import com.argus.api.dto.response.WalletTimelineResponse;
import com.argus.api.dto.response.SyncResponse;
import com.argus.domain.model.AssetTransfer;
import com.argus.domain.model.Wallet;
import com.argus.domain.service.WalletService;
import com.argus.domain.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody WalletRequest request) {
        log.info("POST /api/v1/wallets - Creating wallet with address: {}", request.getAddress());

        Wallet wallet = walletService.createWallet(request.toDomain());

        return ResponseEntity.status(HttpStatus.CREATED).body(WalletResponse.fromDomain(wallet));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> getWalletById(@PathVariable UUID id) {
        log.info("GET /api/v1/wallets/{} - Fetching wallet by id", id);

        Wallet wallet = walletService.getWalletById(id);

        return ResponseEntity.ok(WalletResponse.fromDomain(wallet));
    }

    @GetMapping("/address/{address}")
    public ResponseEntity<WalletResponse> getWalletByAddress(@PathVariable String address) {
        log.info("GET /api/v1/wallets/address/{} - Fetching wallet by address", address);

        Wallet wallet = walletService.getWalletByAddress(address);

        return ResponseEntity.ok(WalletResponse.fromDomain(wallet));
    }

    @GetMapping
    public ResponseEntity<List<WalletResponse>> getAllWallets(
            @RequestParam(required = false) String type) {

        log.info("GET /api/v1/wallets - Fetching all wallets (type filter: {})", type);

        List<Wallet> wallets;

        if (type != null && !type.isEmpty()) {
            Wallet.WalletType walletType = Wallet.WalletType.valueOf(type.toUpperCase());
            wallets = walletService.getWalletsByType(walletType);
        } else {
            wallets = walletService.getAllWallets();
        }

        List<WalletResponse> response = wallets.stream()
                .map(WalletResponse::fromDomain)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WalletResponse> updateWallet(
            @PathVariable UUID id,
            @Valid @RequestBody WalletRequest request) {

        log.info("PUT /api/v1/wallets/{} - Updating wallet", id);

        Wallet updatedWallet = request.toDomain();
        Wallet wallet = walletService.updateWallet(id, updatedWallet);

        return ResponseEntity.ok(WalletResponse.fromDomain(wallet));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWallet(@PathVariable UUID id) {
        log.info("DELETE /api/v1/wallets/{} - Deleting wallet", id);

        walletService.deleteWallet(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/{address}")
    public ResponseEntity<Boolean> walletExists(@PathVariable String address) {
        log.info("GET /api/v1/wallets/exists/{} - Checking if wallet exists", address);

        boolean exists = walletService.walletExists(address);

        return ResponseEntity.ok(exists);
    }

    @GetMapping("/{address}/transactions")
    public ResponseEntity<WalletTimelineResponse> getWalletTimeline(
            @PathVariable @Pattern(regexp = "^0x[a-fA-F0-9]{40}$") String address,
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int limit,
            @RequestParam(defaultValue = "desc") @Pattern(regexp = "^(asc|desc)$") String order) {

        List<AssetTransfer> transfers = transactionService
                .getWalletHistory(address, limit, order);

        long totalCount = transactionService.countTransactions(address);

        WalletTimelineResponse response = WalletTimelineResponse.builder()
                .address(address)
                .totalTransactions(totalCount)
                .showing(transfers.size())
                .transactions(transfers.stream()
                        .map(AssetTransferResponse::fromDomain)
                        .toList())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{address}/sync-history")
    public ResponseEntity<SyncResponse> syncWalletHistory(
            @PathVariable @Pattern(regexp = "^0x[a-fA-F0-9]{40}$") String address,
            @RequestParam(defaultValue = "500") int maxCount) {

        log.info("POST /api/v1/wallets/{}/sync-history - maxCount: {}",
                address, maxCount);

        List<AssetTransfer> synced = transactionService
                .syncWalletHistory(address, maxCount);

        SyncResponse response = SyncResponse.builder()
                .address(address)
                .syncedCount(synced.size())
                .message("Successfully synced wallet transaction history")
                .build();

        return ResponseEntity.ok(response);
    }
}
