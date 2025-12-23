package com.argus.infra.blockchain;

import com.argus.core.exception.BlockchainException;
import com.argus.domain.model.Transaction;
import com.argus.domain.port.blockchain.BlockChainPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.methods.response.EthTransaction;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
public class Web3jBlockchainAdapter implements BlockChainPort {

    private final String rpcUrl;
    private final int timeoutSeconds;
    private final int retryAttempts;

    private Web3j web3j;

    public Web3jBlockchainAdapter(
            @Value("${argus.blockchain.rpc-url}") String rpcUrl,
            @Value("${argus.blockchain.timeout-seconds:30}") int timeoutSeconds,
            @Value("${argus.blockchain.retry-attempts:3}") int retryAttempts) {
        this.rpcUrl = rpcUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.retryAttempts = retryAttempts;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing Web3j blockchain adapter");
        log.info("RPC URL: {}", maskApiKey(rpcUrl));
        log.info("Timeout: {}s, Retries: {}", timeoutSeconds, retryAttempts);

        this.web3j = Web3j.build(new HttpService(rpcUrl));

        try {
            String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            log.info("Connected to blockchain client: {}", clientVersion);
        } catch (Exception e) {
            log.warn("Failed to verify blockchain connection: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (web3j != null) {
            log.info("Shutting down Web3j connection");
            web3j.shutdown();
        }
    }

    @Override
    public long getLatestBlockNumber() {
        log.debug("Fetching latest block number");

        Exception lastException = null;

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                EthBlockNumber ethBlockNumber = web3j.ethBlockNumber().send();

                if (ethBlockNumber.hasError()) {
                    throw new BlockchainException(
                            "RPC error: " + ethBlockNumber.getError().getMessage());
                }

                BigInteger blockNumber = ethBlockNumber.getBlockNumber();
                long blockNum = blockNumber.longValue();

                log.debug("Latest block number: {}", blockNum);
                return blockNum;

            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed to get block number: {}",
                        attempt, retryAttempts, e.getMessage());

                if (attempt < retryAttempts) {
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new BlockchainException(
                "Failed to get latest block number after " + retryAttempts + " attempts",
                lastException);
    }

    @Override
    public Optional<Transaction> getTransactionByHash(String txHash) {
        if (txHash == null || txHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction hash cannot be null or empty");
        }

        log.debug("Fetching transaction by hash: {}", txHash);

        Exception lastException = null;

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                EthTransaction ethTransaction = web3j
                        .ethGetTransactionByHash(txHash).send();

                if (ethTransaction.hasError()) {
                    throw new BlockchainException(
                            "RPC error: " + ethTransaction.getError().getMessage());
                }

                if (!ethTransaction.getTransaction().isPresent()) {
                    log.debug("Transaction not found: {}", txHash);
                    return Optional.empty();
                }

                org.web3j.protocol.core.methods.response.Transaction web3jTx = ethTransaction.getTransaction().get();

                Transaction domainTx = convertToDomainTransaction(web3jTx);

                log.debug("Successfully fetched transaction: {}", txHash);
                return Optional.of(domainTx);

            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed to get transaction {}: {}",
                        attempt, retryAttempts, txHash, e.getMessage());

                if (attempt < retryAttempts) {
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new BlockchainException(
                "Failed to get transaction " + txHash + " after " + retryAttempts + " attempts",
                lastException);
    }

    private Transaction convertToDomainTransaction(
            org.web3j.protocol.core.methods.response.Transaction web3jTx) {

        return Transaction.builder()
                .txHash(web3jTx.getHash())
                .chain("ethereum")
                .blockNumber(web3jTx.getBlockNumber() != null ? web3jTx.getBlockNumber().longValue() : null)
                .gasUsed(web3jTx.getGas() != null ? web3jTx.getGas().longValue() : null)
                .gasPrice(web3jTx.getGasPrice() != null ? new BigDecimal(web3jTx.getGasPrice()) : null)
                .txTimestamp(null)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String maskApiKey(String url) {
        if (url == null)
            return "null";
        return url.replaceAll("(/v2/)([^/]+)", "$1***");
    }
}
