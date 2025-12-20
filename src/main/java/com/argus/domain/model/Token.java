package com.argus.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Token {
    private UUID id;
    private String chain;
    private String symbol;
    private String name;
    private int decimals;
    private BigDecimal marketCap;
    private BigDecimal liquidity;
    private BigDecimal riskScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
