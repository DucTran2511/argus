package com.argus.infra.persistence.adapter;

import com.argus.domain.model.Transaction;
import com.argus.domain.port.persistence.TransactionPersistencePort;
import com.argus.infra.persistence.entity.TransactionEntity;
import com.argus.infra.persistence.repository.TransactionRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionPersistenceAdapter implements TransactionPersistencePort {
    private final TransactionRepository transactionRepository;

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
