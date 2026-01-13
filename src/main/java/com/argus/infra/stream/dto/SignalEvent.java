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
public class SignalEvent {
    private String signalType;
    private String walletAddress;
    private String signalData;
    private String tokenAddress;
    private String tokenSymbol;
    private BigDecimal amount;
    private BigDecimal usdValue;
    private LocalDateTime detectedAt;
}
