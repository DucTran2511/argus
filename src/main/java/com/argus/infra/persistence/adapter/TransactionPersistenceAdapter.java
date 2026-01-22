package com.argus.infra.persistence.adapter;

import com.argus.domain.model.Transaction;
import com.argus.domain.model.AssetTransfer;
import com.argus.domain.port.persistence.TransactionPersistencePort;
import com.argus.infra.persistence.entity.TransactionEntity;
import com.argus.infra.persistence.entity.AssetTransferEntity;
import com.argus.infra.persistence.repository.TransactionRepository;
import com.argus.infra.persistence.repository.AssetTransferRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionPersistenceAdapter implements TransactionPersistencePort {
    private final TransactionRepository transactionRepository;
    private final AssetTransferRepository assetTransferRepository;

    @Override
    @Transactional
    public Transaction save(Transaction transaction) {
        validateTransaction(transaction);

        TransactionEntity entity = toEntity(transaction);
        TransactionEntity saved = transactionRepository.save(entity);
        return toDomain(saved);
    }

    private void validateTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
    }

    @Override
    @Transactional
    public List<AssetTransfer> saveAssetTransfers(List<AssetTransfer> transfers) {
        List<AssetTransferEntity> toSave = transfers.stream()
                .map(this::toAssetTransferEntity)
                .filter(e -> !assetTransferRepository.existsByTxHashAndWalletAddressAndCategory(
                        e.getTxHash(), e.getWalletAddress(), e.getCategory()))
                .toList();

        if (toSave.isEmpty()) {
            log.info("No new transfers to save");
            return List.of();
        }

        List<AssetTransferEntity> saved = assetTransferRepository.saveAll(toSave);
        return saved.stream().map(this::toAssetTransferDomain).toList();
    }

    @Override
    public List<AssetTransfer> findByWalletAddress(String address, int limit, String order) {
        Sort sort = order.equalsIgnoreCase("asc")
                ? Sort.by("txTimestamp").ascending()
                : Sort.by("txTimestamp").descending();

        List<AssetTransferEntity> entities = assetTransferRepository
                .findByWalletAddressOrderByTxTimestampDesc(
                        address.toLowerCase(),
                        PageRequest.of(0, limit, sort));

        return entities.stream()
                .map(this::toAssetTransferDomain)
                .toList();
    }

    @Override
    public List<AssetTransfer> findByWalletAddressAndDateRange(
            String address, LocalDateTime from, LocalDateTime to) {
        return assetTransferRepository
                .findByWalletAddressAndTxTimestampBetween(address.toLowerCase(), from, to)
                .stream()
                .map(this::toAssetTransferDomain)
                .toList();
    }

    @Override
    public long countByWalletAddress(String address) {
        return assetTransferRepository.countByWalletAddress(address.toLowerCase());
    }

    @Override
    public List<AssetTransfer> findByPriceSourceIn(List<String> priceSources) {
        return assetTransferRepository.findByPriceSourceIn(priceSources)
                .stream()
                .map(this::toAssetTransferDomain)
                .toList();
    }

    private AssetTransferEntity toAssetTransferEntity(AssetTransfer domain) {
        return AssetTransferEntity.builder()
                .walletAddress(domain.getWalletAddress())
                .txHash(domain.getTxHash())
                .blockNumber(domain.getBlockNumber())
                .fromAddress(domain.getFrom())
                .toAddress(domain.getTo())
                .category(domain.getCategory().name())
                .value(domain.getValue())
                .assetSymbol(domain.getAssetSymbol())
                .tokenAddress(domain.getTokenAddress())
                .txTimestamp(domain.getTxTimestamp())
                .createdAt(domain.getCreatedAt())
                .priceAtTx(domain.getPriceAtTx())
                .priceSource(domain.getPriceSource())
                .usdValue(domain.getUsdValue())
                .build();
    }

    private AssetTransfer toAssetTransferDomain(AssetTransferEntity entity) {
        return AssetTransfer.builder()
                .id(entity.getId())
                .walletAddress(entity.getWalletAddress())
                .txHash(entity.getTxHash())
                .blockNumber(entity.getBlockNumber())
                .from(entity.getFromAddress())
                .to(entity.getToAddress())
                .category(AssetTransfer.TransferCategory.valueOf(entity.getCategory()))
                .value(entity.getValue())
                .assetSymbol(entity.getAssetSymbol())
                .tokenAddress(entity.getTokenAddress())
                .txTimestamp(entity.getTxTimestamp())
                .createdAt(entity.getCreatedAt())
                .priceAtTx(entity.getPriceAtTx())
                .priceSource(entity.getPriceSource())
                .usdValue(entity.getUsdValue())
                .build();
    }

    private Transaction toDomain(TransactionEntity entity) {
        return Transaction.builder()
                .id(entity.getId())
                .walletId(entity.getWalletId())
                .chain(entity.getChain())
                .type(entity.getType())
                .txHash(entity.getTxHash())
                .from(entity.getFrom())
                .to(entity.getTo())
                .value(entity.getValue())
                .input(entity.getInput())
                .tokenIn(entity.getTokenIn())
                .tokenOut(entity.getTokenOut())
                .amountIn(entity.getAmountIn())
                .amountOut(entity.getAmountOut())
                .usdValue(entity.getUsdValue())
                .blockNumber(entity.getBlockNumber())
                .gasUsed(entity.getGasUsed())
                .gasPrice(entity.getGasPrice())
                .txTimestamp(entity.getTxTimestamp())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private TransactionEntity toEntity(Transaction transaction) {
        return TransactionEntity.builder()
                .id(transaction.getId())
                .walletId(transaction.getWalletId())
                .chain(transaction.getChain())
                .type(transaction.getType())
                .txHash(transaction.getTxHash())
                .from(transaction.getFrom())
                .to(transaction.getTo())
                .value(transaction.getValue())
                .input(transaction.getInput())
                .tokenIn(transaction.getTokenIn())
                .tokenOut(transaction.getTokenOut())
                .amountIn(transaction.getAmountIn())
                .amountOut(transaction.getAmountOut())
                .usdValue(transaction.getUsdValue())
                .blockNumber(transaction.getBlockNumber())
                .gasUsed(transaction.getGasUsed())
                .gasPrice(transaction.getGasPrice())
                .txTimestamp(transaction.getTxTimestamp())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
