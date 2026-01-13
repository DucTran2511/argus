package com.argus.infra.stream.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private String txHash;
    private String from;
    private String to;
    private BigDecimal value;
    private Long blockNumber;
    private LocalDateTime timestamp;
    private String category;
    private String tokenAddress; // For token identification
    private BigDecimal usdValue; // Already enriched USD value
}
