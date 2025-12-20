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
@NoArgsConstructor
@AllArgsConstructor
public class Signal {
    private Long id;
    private String type;
    private UUID walletId;
    private String tokenAddress;
    private String tokenSymbol;
    private String chain;
    private BigDecimal usdValue;
    private BigDecimal confidenceScore;
    private String aiNarrative;
    private String metadata;
    private LocalDateTime createdAt;
}
