package com.argus.domain.service;

import com.argus.domain.model.AssetTransfer;
import com.argus.domain.model.AssetTransfer.TransferCategory;
import com.argus.domain.model.TokenPriceRange;
import com.argus.domain.port.blockchain.BlockChainPort;
import com.argus.domain.port.blockchain.PricePort;
import com.argus.domain.port.persistence.TransactionPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalImportService {

    private static final int DEFAULT_MAX_TRANSFERS = 500;
    private static final List<TransferCategory> IMPORT_CATEGORIES = List.of(
            TransferCategory.EXTERNAL,
            TransferCategory.ERC20);

    private final BlockChainPort blockChainPort;
    private final PricePort pricePort;
    private final TransactionPersistencePort transactionPort;
    private final PriceService priceService;

    public ImportResult importWalletHistory(String address, int days) {
        log.info("Starting historical import for wallet: {} ({} days)", address, days);

        List<AssetTransfer> transfers = blockChainPort.getWalletTransactions(
                address, IMPORT_CATEGORIES, DEFAULT_MAX_TRANSFERS);

        if (transfers.isEmpty()) {
            log.info("No transfers found for wallet: {}", address);
            return new ImportResult(0, 0, 0, 0, 0);
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<AssetTransfer> recentTransfers = transfers.stream()
                .filter(t -> t.getTxTimestamp() != null && t.getTxTimestamp().isAfter(cutoff))
                .toList();

        log.info("Found {} transfers in last {} days", recentTransfers.size(), days);

        Set<String> uniqueTokens = recentTransfers.stream()
                .map(AssetTransfer::getTokenAddress)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        log.info("Unique tokens to fetch prices for: {}", uniqueTokens.size());

        LocalDateTime from = LocalDateTime.now().minusDays(days);
        LocalDateTime to = LocalDateTime.now();
        Map<String, TokenPriceRange> priceRanges = fetchPriceRanges(uniqueTokens, from, to);
        List<AssetTransfer> enrichedTransfers = enrichTransfers(recentTransfers, priceRanges);

        List<AssetTransfer> saved = transactionPort.saveAssetTransfers(enrichedTransfers);

        long priceHits = saved.stream()
                .filter(t -> "coingecko".equals(t.getPriceSource()))
                .count();
        long priceMissing = saved.stream()
                .filter(t -> "missing".equals(t.getPriceSource()) || "unsupported".equals(t.getPriceSource()))
                .count();
        long nativeEth = saved.stream()
                .filter(t -> t.getTokenAddress() == null)
                .count();

        log.info("Import complete for {}: {} transfers, {} price hits, {} missing, {} native ETH",
                address, saved.size(), priceHits, priceMissing, nativeEth);

        return new ImportResult(
                saved.size(),
                uniqueTokens.size(),
                (int) priceHits,
                (int) priceMissing,
                (int) nativeEth);
    }

    private Map<String, TokenPriceRange> fetchPriceRanges(
            Set<String> tokenAddresses,
            LocalDateTime from,
            LocalDateTime to) {

        Map<String, TokenPriceRange> ranges = new HashMap<>();

        for (String tokenAddr : tokenAddresses) {
            try {
                Optional<TokenPriceRange> range = pricePort.getTokenPriceRange(tokenAddr, from, to);
                range.ifPresent(r -> ranges.put(tokenAddr, r));
            } catch (Exception e) {
                log.warn("Failed to fetch price range for token {}: {}", tokenAddr, e.getMessage());
            }
        }

        log.info("Fetched price ranges for {} out of {} tokens", ranges.size(), tokenAddresses.size());
        return ranges;
    }

    private List<AssetTransfer> enrichTransfers(
            List<AssetTransfer> transfers,
            Map<String, TokenPriceRange> priceRanges) {

        return transfers.stream().map(transfer -> {
            if (transfer.getTokenAddress() == null) {
                return enrichNativeEthTransfer(transfer);
            }

            TokenPriceRange range = priceRanges.get(transfer.getTokenAddress());

            if (range == null) {
                return AssetTransfer.builder()
                        .id(transfer.getId())
                        .walletAddress(transfer.getWalletAddress())
                        .txHash(transfer.getTxHash())
                        .blockNumber(transfer.getBlockNumber())
                        .from(transfer.getFrom())
                        .to(transfer.getTo())
                        .category(transfer.getCategory())
                        .value(transfer.getValue())
                        .assetSymbol(transfer.getAssetSymbol())
                        .tokenAddress(transfer.getTokenAddress())
                        .txTimestamp(transfer.getTxTimestamp())
                        .createdAt(transfer.getCreatedAt())
                        .priceAtTx(null)
                        .priceSource("unsupported")
                        .usdValue(null)
                        .build();
            }

            BigDecimal priceAtTx = range.getPriceAtTimestamp(transfer.getTxTimestamp());

            if (priceAtTx == null) {
                return AssetTransfer.builder()
                        .id(transfer.getId())
                        .walletAddress(transfer.getWalletAddress())
                        .txHash(transfer.getTxHash())
                        .blockNumber(transfer.getBlockNumber())
                        .from(transfer.getFrom())
                        .to(transfer.getTo())
                        .category(transfer.getCategory())
                        .value(transfer.getValue())
                        .assetSymbol(transfer.getAssetSymbol())
                        .tokenAddress(transfer.getTokenAddress())
                        .txTimestamp(transfer.getTxTimestamp())
                        .createdAt(transfer.getCreatedAt())
                        .priceAtTx(null)
                        .priceSource("missing")
                        .usdValue(null)
                        .build();
            }

            BigDecimal usdValue = transfer.getValue() != null
                    ? transfer.getValue().multiply(priceAtTx)
                    : null;

            return AssetTransfer.builder()
                    .id(transfer.getId())
                    .walletAddress(transfer.getWalletAddress())
                    .txHash(transfer.getTxHash())
                    .blockNumber(transfer.getBlockNumber())
                    .from(transfer.getFrom())
                    .to(transfer.getTo())
                    .category(transfer.getCategory())
                    .value(transfer.getValue())
                    .assetSymbol(transfer.getAssetSymbol())
                    .tokenAddress(transfer.getTokenAddress())
                    .txTimestamp(transfer.getTxTimestamp())
                    .createdAt(transfer.getCreatedAt())
                    .priceAtTx(priceAtTx)
                    .priceSource("coingecko")
                    .usdValue(usdValue)
                    .build();

        }).toList();
    }

    private AssetTransfer enrichNativeEthTransfer(AssetTransfer transfer) {
        BigDecimal ethPrice = priceService.getEthPrice();
        BigDecimal usdValue = transfer.getValue() != null && ethPrice != null
                ? transfer.getValue().multiply(ethPrice)
                : null;

        return AssetTransfer.builder()
                .id(transfer.getId())
                .walletAddress(transfer.getWalletAddress())
                .txHash(transfer.getTxHash())
                .blockNumber(transfer.getBlockNumber())
                .from(transfer.getFrom())
                .to(transfer.getTo())
                .category(transfer.getCategory())
                .value(transfer.getValue())
                .assetSymbol(transfer.getAssetSymbol())
                .tokenAddress(transfer.getTokenAddress())
                .txTimestamp(transfer.getTxTimestamp())
                .createdAt(transfer.getCreatedAt())
                .priceAtTx(ethPrice)
                .priceSource("current")
                .usdValue(usdValue)
                .build();
    }

    public record ImportResult(
            int transferCount,
            int uniqueTokens,
            int priceHits,
            int priceMissing,
            int nativeEthCount) {
    }
}
