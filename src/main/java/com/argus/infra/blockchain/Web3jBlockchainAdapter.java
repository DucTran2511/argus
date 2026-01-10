package com.argus.infra.blockchain;

import com.argus.core.exception.BlockchainException;
import com.argus.domain.model.AssetTransfer;
import com.argus.domain.model.AssetTransfer.TransferCategory;
import com.argus.domain.model.Transaction;
import com.argus.domain.port.blockchain.BlockChainPort;
import com.argus.infra.blockchain.dto.AlchemyAssetTransfersRequest;
import com.argus.infra.blockchain.dto.AlchemyTransferDto;
import com.argus.infra.blockchain.dto.response.AlchemyAssetTransfersResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.DefaultBlockParameter;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

@Slf4j
@Component
public class Web3jBlockchainAdapter implements BlockChainPort {

    private final String rpcUrl;
    private final int timeoutSeconds;
    private final int retryAttempts;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService alchemyExecutor;

    private Web3j web3j;

    public Web3jBlockchainAdapter(
            @Value("${argus.blockchain.rpc-url}") String rpcUrl,
            @Value("${argus.blockchain.timeout-seconds:30}") int timeoutSeconds,
            @Value("${argus.blockchain.retry-attempts:3}") int retryAttempts) {
        this.rpcUrl = rpcUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.retryAttempts = retryAttempts;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
        this.alchemyExecutor = Executors.newFixedThreadPool(4);
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
        if (alchemyExecutor != null) {
            alchemyExecutor.shutdown();
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

    @Override
    public List<Transaction> getTransactionsByBlock(long blockNumber) {
        log.debug("Fetching transactions for block: {}", blockNumber);

        Exception lastException = null;

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                EthBlock ethBlock = web3j.ethGetBlockByNumber(
                        DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNumber)),
                        true).send();

                if (ethBlock.hasError()) {
                    throw new BlockchainException(
                            "RPC error: " + ethBlock.getError().getMessage());
                }

                if (ethBlock.getBlock() == null) {
                    log.warn("Block {} not found", blockNumber);
                    return java.util.Collections.emptyList();
                }

                EthBlock.Block block = ethBlock.getBlock();

                List<Transaction> transactions = new ArrayList<>();

                for (EthBlock.TransactionResult txResult : block
                        .getTransactions()) {
                    if (txResult instanceof EthBlock.TransactionObject) {
                        EthBlock.TransactionObject txObj = (EthBlock.TransactionObject) txResult;

                        org.web3j.protocol.core.methods.response.Transaction web3jTx = txObj.get();
                        Transaction domainTx = convertToDomainTransaction(web3jTx);
                        transactions.add(domainTx);
                    }
                }

                log.debug("Successfully fetched {} transactions from block {}", transactions.size(), blockNumber);
                return transactions;

            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed to get transactions for block {}: {}",
                        attempt, retryAttempts, blockNumber, e.getMessage());

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
                "Failed to get transactions for block " + blockNumber + " after " + retryAttempts + " attempts",
                lastException);
    }

    @Override
    public List<AssetTransfer> getWalletTransactions(
            String address,
            List<TransferCategory> categories,
            int maxCount) {

        log.info("Fetching asset transfers for wallet: {}", address);

        List<String> categoryStrings = categories.stream()
                .map(c -> c.name().toLowerCase())
                .toList();

        CompletableFuture<List<AlchemyTransferDto>> outgoingFuture = CompletableFuture
                .supplyAsync(() -> fetchAllTransfersWithPagination(address, null, categoryStrings, maxCount),
                        alchemyExecutor);

        CompletableFuture<List<AlchemyTransferDto>> incomingFuture = CompletableFuture
                .supplyAsync(() -> fetchAllTransfersWithPagination(null, address, categoryStrings, maxCount),
                        alchemyExecutor);

        List<AlchemyTransferDto> outgoing = outgoingFuture.join();
        List<AlchemyTransferDto> incoming = incomingFuture.join();

        log.info("Fetched {} outgoing + {} incoming transfers", outgoing.size(), incoming.size());

        List<AssetTransfer> merged = mergeAndDeduplicate(outgoing, incoming, address);

        log.info("After deduplication: {} unique transfers", merged.size());

        return merged;
    }

    private List<AlchemyTransferDto> fetchAllTransfersWithPagination(
            String fromAddress,
            String toAddress,
            List<String> categories,
            int maxTotal) {

        List<AlchemyTransferDto> allTransfers = new ArrayList<>();
        String pageKey = null;
        int perPage = Math.min(maxTotal, 1000);

        do {
            AlchemyAssetTransfersRequest request = AlchemyAssetTransfersRequest.builder()
                    .fromAddress(fromAddress)
                    .toAddress(toAddress)
                    .categories(categories)
                    .maxCount(String.format("0x%x", perPage))
                    .fromBlock("0x0")
                    .toBlock("latest")
                    .withMetadata(true)
                    .order("desc")
                    .pageKey(pageKey)
                    .build();

            AlchemyAssetTransfersResponse response = sendAlchemyRequest(request);

            if (response.getTransfers() != null) {
                allTransfers.addAll(response.getTransfers());
            }

            pageKey = response.getPageKey();

            int fetchedCount = response.getTransfers() != null ? response.getTransfers().size() : 0;
            log.debug("Fetched {} transfers, pageKey: {}", fetchedCount, pageKey != null ? "exists" : "null");

            if (allTransfers.size() >= maxTotal) {
                break;
            }

        } while (pageKey != null);

        return allTransfers;
    }

    private List<AssetTransfer> mergeAndDeduplicate(
            List<AlchemyTransferDto> outgoing,
            List<AlchemyTransferDto> incoming,
            String walletAddress) {

        Map<String, AssetTransfer> uniqueTransfers = new LinkedHashMap<>();

        for (AlchemyTransferDto dto : outgoing) {
            String key = createDedupeKey(dto);
            if (!uniqueTransfers.containsKey(key)) {
                uniqueTransfers.put(key, normalizeTransfer(dto, walletAddress));
            }
        }

        for (AlchemyTransferDto dto : incoming) {
            String key = createDedupeKey(dto);
            if (!uniqueTransfers.containsKey(key)) {
                uniqueTransfers.put(key, normalizeTransfer(dto, walletAddress));
            }
        }

        return new ArrayList<>(uniqueTransfers.values());
    }

    private String createDedupeKey(AlchemyTransferDto dto) {
        return String.format("%s|%s|%d",
                dto.getHash(),
                dto.getCategory(),
                dto.getLogIndex() != null ? dto.getLogIndex() : 0);
    }

    private AssetTransfer normalizeTransfer(AlchemyTransferDto dto, String walletAddress) {
        return AssetTransfer.builder()
                .walletAddress(walletAddress.toLowerCase())
                .txHash(dto.getHash())
                .blockNumber(parseHexToLong(dto.getBlockNum()))
                .from(normalizeAddress(dto.getFrom()))
                .to(normalizeAddress(dto.getTo()))
                .value(parseHexToBigDecimal(dto.getValue()))
                .assetSymbol(dto.getAsset())
                .tokenAddress(dto.getRawContract() != null ? dto.getRawContract().getAddress() : null)
                .category(mapCategory(dto.getCategory()))
                .txTimestamp(dto.getMetadata() != null
                        ? parseIsoTimestamp(dto.getMetadata().getBlockTimestamp())
                        : null)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TransferCategory mapCategory(String category) {
        return switch (category.toLowerCase()) {
            case "external" -> TransferCategory.EXTERNAL;
            case "internal" -> TransferCategory.INTERNAL;
            case "erc20" -> TransferCategory.ERC20;
            case "erc721" -> TransferCategory.ERC721;
            case "erc1155" -> TransferCategory.ERC1155;
            default -> TransferCategory.EXTERNAL;
        };
    }

    private BigDecimal parseHexToBigDecimal(String hexValue) {
        if (hexValue == null || hexValue.isEmpty() || hexValue.equals("0x0")) {
            return BigDecimal.ZERO;
        }
        String clean = hexValue.startsWith("0x") ? hexValue.substring(2) : hexValue;
        try {
            return new BigDecimal(new BigInteger(clean, 16));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse hex value to BigDecimal: {}", hexValue);
            return BigDecimal.ZERO;
        }
    }

    private AlchemyAssetTransfersResponse sendAlchemyRequest(AlchemyAssetTransfersRequest request) {
        try {
            Map<String, Object> params = new HashMap<>();
            if (request.getFromAddress() != null)
                params.put("fromAddress", request.getFromAddress());
            if (request.getToAddress() != null)
                params.put("toAddress", request.getToAddress());
            params.put("fromBlock", request.getFromBlock());
            params.put("toBlock", request.getToBlock());
            params.put("category", request.getCategories());
            params.put("maxCount", request.getMaxCount());
            params.put("withMetadata", request.getWithMetadata());
            params.put("order", request.getOrder());
            if (request.getPageKey() != null)
                params.put("pageKey", request.getPageKey());

            Map<String, Object> rpcRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 1,
                    "method", "alchemy_getAssetTransfers",
                    "params", List.of(params));

            String jsonBody = objectMapper.writeValueAsString(rpcRequest);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(rpcUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Alchemy HTTP error {}: {}", response.statusCode(), response.body());
                throw new BlockchainException("Alchemy returned HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode error = root.get("error");
            if (error != null && !error.isNull()) {
                String errorMsg = error.has("message") ? error.get("message").asText() : "Unknown error";
                int errorCode = error.has("code") ? error.get("code").asInt() : -1;
                throw new BlockchainException("Alchemy API error [" + errorCode + "]: " + errorMsg);
            }

            JsonNode result = root.get("result");
            if (result == null || result.isNull()) {
                throw new BlockchainException("Alchemy returned null result");
            }

            return objectMapper.treeToValue(result, AlchemyAssetTransfersResponse.class);

        } catch (Exception e) {
            log.error("Failed to fetch asset transfers from Alchemy: {}", e.getMessage());
            throw new BlockchainException("Alchemy API call failed", e);
        }
    }

    private LocalDateTime parseIsoTimestamp(String isoTimestamp) {
        if (isoTimestamp == null)
            return null;
        return LocalDateTime.parse(isoTimestamp, DateTimeFormatter.ISO_DATE_TIME);
    }

    private long parseHexToLong(String hex) {
        if (hex == null || hex.isEmpty()) {
            return 0L;
        }
        String clean = hex.startsWith("0x") ? hex.substring(2) : hex;
        try {
            return Long.parseLong(clean, 16);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse hex: {}", hex);
            return 0L;
        }
    }

    private String normalizeAddress(String address) {
        return address != null ? address.toLowerCase() : null;
    }

    private Transaction convertToDomainTransaction(
            org.web3j.protocol.core.methods.response.Transaction web3jTx) {

        // Convert Wei to ETH (1 ETH = 10^18 Wei)
        BigDecimal valueInEth = null;
        if (web3jTx.getValue() != null) {
            BigDecimal weiValue = new BigDecimal(web3jTx.getValue());
            // Divide by 10^18 to convert Wei to ETH
            valueInEth = weiValue.divide(new BigDecimal("1000000000000000000"), 18, java.math.RoundingMode.HALF_UP);
        }

        return Transaction.builder()
                .txHash(web3jTx.getHash())
                .chain("ethereum")

                // Core transaction data
                .from(web3jTx.getFrom())
                .to(web3jTx.getTo()) // Can be null for contract creation
                .value(valueInEth)
                .input(web3jTx.getInput())

                // Block metadata
                .blockNumber(web3jTx.getBlockNumber() != null ? web3jTx.getBlockNumber().longValue() : null)
                .gasUsed(web3jTx.getGas() != null ? web3jTx.getGas().longValue() : null)
                .gasPrice(web3jTx.getGasPrice() != null ? new BigDecimal(web3jTx.getGasPrice()) : null)

                // Timestamps
                .txTimestamp(null) // Will be set when we fetch block timestamp
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String maskApiKey(String url) {
        if (url == null)
            return "null";
        return url.replaceAll("(/v2/)([^/]+)", "$1***");
    }

    /**
     * Decodes ERC20 Transfer events from a transaction receipt.
     * 
     * ERC20 Transfer Event:
     * - Event signature: Transfer(address indexed from, address indexed to, uint256
     * value)
     * - Topic0 (keccak256 hash):
     * 0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef
     * - Topic1: from address (indexed)
     * - Topic2: to address (indexed)
     * - Data: amount (uint256)
     * 
     * @param txHash Transaction hash to fetch receipt for
     * @return List of decoded ERC20 Transfer events
     */
    public java.util.List<Erc20TransferEvent> decodeErc20TransferEvents(String txHash) {
        // ERC20 Transfer event signature hash
        final String TRANSFER_EVENT_SIGNATURE = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

        java.util.List<Erc20TransferEvent> events = new java.util.ArrayList<>();

        try {
            // Fetch transaction receipt to get logs
            org.web3j.protocol.core.methods.response.EthGetTransactionReceipt receiptResponse = web3j
                    .ethGetTransactionReceipt(txHash).send();

            if (receiptResponse.getTransactionReceipt().isEmpty()) {
                log.debug("No receipt found for transaction: {}", txHash);
                return events;
            }

            org.web3j.protocol.core.methods.response.TransactionReceipt receipt = receiptResponse
                    .getTransactionReceipt().get();

            // Iterate through all logs in the receipt
            for (org.web3j.protocol.core.methods.response.Log eventLog : receipt.getLogs()) {
                // Check if this is a Transfer event (topic0 matches)
                if (eventLog.getTopics() != null &&
                        !eventLog.getTopics().isEmpty() &&
                        eventLog.getTopics().get(0).equalsIgnoreCase(TRANSFER_EVENT_SIGNATURE)) {

                    try {
                        Erc20TransferEvent event = decodeTransferEvent(eventLog, txHash, receipt.getBlockNumber());
                        events.add(event);
                    } catch (Exception e) {
                        log.warn("Failed to decode Transfer event in tx {}: {}", txHash, e.getMessage());
                    }
                }
            }

            log.debug("Decoded {} ERC20 Transfer events from tx: {}", events.size(), txHash);

        } catch (Exception e) {
            log.error("Failed to fetch receipt for tx {}: {}", txHash, e.getMessage());
        }

        return events;
    }

    /**
     * Decodes a single ERC20 Transfer event from a log entry.
     */
    private Erc20TransferEvent decodeTransferEvent(
            org.web3j.protocol.core.methods.response.Log eventLog,
            String txHash,
            BigInteger blockNumber) {

        // Extract token contract address
        String tokenAddress = eventLog.getAddress();

        // Extract 'from' address from topic1 (remove leading zeros and 0x prefix)
        String from = "0x" + eventLog.getTopics().get(1).substring(26);

        // Extract 'to' address from topic2 (remove leading zeros and 0x prefix)
        String to = "0x" + eventLog.getTopics().get(2).substring(26);

        // Extract amount from data field (remove 0x prefix)
        String amountHex = eventLog.getData().substring(2);
        BigDecimal amount = new BigDecimal(new BigInteger(amountHex, 16));

        return Erc20TransferEvent.builder()
                .tokenAddress(tokenAddress)
                .from(from)
                .to(to)
                .amount(amount)
                .decimals(18) // Default to 18, can be fetched from token contract later
                .txHash(txHash)
                .blockNumber(blockNumber != null ? blockNumber.longValue() : null)
                .logIndex(eventLog.getLogIndex() != null ? eventLog.getLogIndex().intValue() : null)
                .build();
    }
}
