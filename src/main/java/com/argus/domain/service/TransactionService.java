package com.argus.domain.service;

import com.argus.core.exception.TransactionNotFoundException;
import com.argus.domain.model.AssetTransfer;
import com.argus.domain.model.AssetTransfer.TransferCategory;
import com.argus.domain.model.DecodedSwap;
import com.argus.domain.model.Transaction;
import com.argus.domain.model.TransactionWithSwap;
import com.argus.domain.port.blockchain.BlockChainPort;
import com.argus.domain.port.blockchain.DexDecoderPort;
import com.argus.domain.port.persistence.TransactionPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class TransactionService {

        private final BlockChainPort blockChainPort;
        private final TransactionPersistencePort transactionPersistencePort;
        private final DexDecoderPort dexDecoder;
        private final PriceService priceService;

        /**
         * Fetch a transaction from the blockchain and decode swap information.
         * 
         * @param txHash Transaction hash
         * @return Transaction with decoded swap info (if applicable)
         * @throws TransactionNotFoundException if transaction doesn't exist
         */
        public TransactionWithSwap getTransaction(String txHash) {
                log.info("Fetching transaction: {}", txHash);

                // Fetch from blockchain
                Transaction tx = blockChainPort.getTransactionByHash(txHash)
                                .orElseThrow(() -> new TransactionNotFoundException(txHash));

                // Decode swap (returns null if not a swap)
                DecodedSwap swap = dexDecoder.decodeSwap(
                                tx.getTo(),
                                tx.getInput(),
                                tx.getValue());

                if (swap != null) {
                        log.info("Decoded {} swap: {} -> {}",
                                        swap.getFunctionName(),
                                        swap.getTokenIn(),
                                        swap.getTokenOut());
                }

                return TransactionWithSwap.builder()
                                .transaction(tx)
                                .decodedSwap(swap)
                                .build();
        }

        /**
         * Fetch, decode, and save a transaction to the database.
         * 
         * @param txHash Transaction hash
         * @return Saved transaction with decoded swap info
         * @throws TransactionNotFoundException if transaction doesn't exist
         */
        public TransactionWithSwap saveTransaction(String txHash) {
                log.info("Saving transaction: {}", txHash);

                // Fetch from blockchain
                Transaction tx = blockChainPort.getTransactionByHash(txHash)
                                .orElseThrow(() -> new TransactionNotFoundException(txHash));

                // Save to database
                Transaction saved = transactionPersistencePort.save(tx);
                log.info("Saved transaction with ID: {}", saved.getId());

                // Decode swap
                DecodedSwap swap = dexDecoder.decodeSwap(
                                saved.getTo(),
                                saved.getInput(),
                                saved.getValue());

                if (swap != null) {
                        log.info("Decoded {} swap with {} hops",
                                        swap.getFunctionName(),
                                        swap.getHopCount());
                }

                return TransactionWithSwap.builder()
                                .transaction(saved)
                                .decodedSwap(swap)
                                .build();
        }

        public List<AssetTransfer> syncWalletHistory(String address, int maxCount) {
                log.info("Syncing wallet history for: {}", address);

                // Fetch from Alchemy
                List<AssetTransfer> transfers = blockChainPort.getWalletTransactions(
                                address,
                                List.of(TransferCategory.EXTERNAL, TransferCategory.ERC20),
                                maxCount);

                // Enrich with USD values
                List<AssetTransfer> enrichedTransfers = new ArrayList<>();
                for (AssetTransfer transfer : transfers) {
                        BigDecimal usdValue;

                        if (transfer.getTokenAddress() == null) {
                                // Native ETH - use ETH price directly
                                BigDecimal ethPrice = priceService.getEthPrice();
                                usdValue = transfer.getValue() != null && ethPrice != null
                                                ? transfer.getValue().multiply(ethPrice)
                                                : BigDecimal.ZERO;
                        } else {
                                // ERC20 token
                                usdValue = priceService.calculateUsdValue(
                                                transfer.getTokenAddress(),
                                                transfer.getValue());
                        }

                        AssetTransfer enriched = AssetTransfer.builder()
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
                                        .usdValue(usdValue)
                                        .build();

                        enrichedTransfers.add(enriched);
                }

                // Save to database
                List<AssetTransfer> saved = transactionPersistencePort.saveAssetTransfers(enrichedTransfers);

                log.info("Synced {} transfers for wallet: {} (with USD values)", saved.size(), address);
                return saved;
        }

        public List<AssetTransfer> getWalletHistory(String address, int limit, String order) {
                log.debug("Fetching wallet history for: {}", address);
                return transactionPersistencePort.findByWalletAddress(address, limit, order);
        }

        /**
         * Count total transactions for a wallet.
         */
        public long countTransactions(String address) {
                return transactionPersistencePort.countByWalletAddress(address);
        }
}
