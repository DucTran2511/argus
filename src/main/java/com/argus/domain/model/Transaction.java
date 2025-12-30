package com.argus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
    private Long id;
    private UUID walletId;
    private String txHash;
    private String chain;
    private String type;

    // Core transaction fields from blockchain
    private String from; // Sender address
    private String to; // Recipient address (null for contract creation)
    private BigDecimal value; // ETH value transferred (in Wei, converted to ETH)
    private String input; // Transaction input data (contract call data)

    // DEX-specific fields (for swap detection)
    private String tokenIn;
    private String tokenOut;
    private BigDecimal amountIn;
    private BigDecimal amountOut;
    private BigDecimal usdValue;

    // Block metadata
    private Long blockNumber;
    private Long gasUsed;
    private BigDecimal gasPrice;
    private LocalDateTime txTimestamp;
    private LocalDateTime createdAt;
}
