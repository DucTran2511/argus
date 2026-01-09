package com.argus.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Value;
import lombok.Builder;

@Value
@Builder
public class TokenPrice {
    private String tokenAddress;
    private String symbol;
    private BigDecimal priceUsd;
    private BigDecimal priceNative;
    private BigDecimal liquidityUsd;
    private String dexId;
    private String pairAddress;
    private LocalDateTime fetchAt;
    private PriceStatus status;

    public enum PriceStatus {
        FOUND, // Valid price
        NOT_FOUND, // Token not on DexScreener
        LOW_LIQUIDITY, // Found but liquidity < $1000
        STALE // Cached but older than 5 min
    }
}
