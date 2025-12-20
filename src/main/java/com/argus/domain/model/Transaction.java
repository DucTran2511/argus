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
    private String tokenIn;
    private String tokenOut;
    private BigDecimal amountIn;
    private BigDecimal amountOut;
    private BigDecimal usdValue;
    private Long blockNumber;
    private Long gasUsed;
    private BigDecimal gasPrice;
    private LocalDateTime txTimestamp;
    private LocalDateTime createdAt;
}
