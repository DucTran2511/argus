package com.argus.api.dto.response;

import com.argus.domain.model.WalletMetrics;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record SmartMoneyResponse(
        String address,
        String archetype,
        String tier,
        BigDecimal totalScore,
        BigDecimal pnlScore,
        BigDecimal consistencyScore,
        BigDecimal convictionScore,
        BigDecimal avgPositionSizeUsd,
        Integer avgHoldTimeSec,
        LocalDateTime lastTradeAt) {
    public static SmartMoneyResponse fromDomain(WalletMetrics metrics) {
        if (metrics == null)
            return null;
        return SmartMoneyResponse.builder()
                .address(metrics.getWalletAddress())
                .archetype(metrics.getArchetype() != null ? metrics.getArchetype().name() : "UNKNOWN")
                .tier(metrics.getTier())
                .totalScore(metrics.getTotalScore())
                .pnlScore(metrics.getPnlScore())
                .consistencyScore(metrics.getConsistencyScore())
                .convictionScore(metrics.getConvictionScore())
                .avgPositionSizeUsd(metrics.getAvgPositionSizeUsd())
                .avgHoldTimeSec(metrics.getAvgHoldTimeSec())
                .lastTradeAt(metrics.getLastTradeAt())
                .build();
    }
}
