package com.argus.domain.model.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class WhaleDetectionRequest {
    String txHash;
    String from;
    String to;
    UUID walletId;
    String walletAddress;
    String tokenAddress;
    String tokenSymbol;
    BigDecimal usdValue;
    LocalDateTime timestamp;
}
