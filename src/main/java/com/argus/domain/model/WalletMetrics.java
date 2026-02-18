package com.argus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletMetrics {
    private String walletAddress;
    private SmartMoneyArchetype archetype;
    private Boolean isBlacklisted;
    private BigDecimal avgPositionSizeUsd;
    private BigDecimal tradeFrequencyPerMonth;
    private Integer tradeCount7d;
    private BigDecimal maxRoiPercent;
    private BigDecimal profitFactor;
    private Integer avgHoldTimeSec;
    private BigDecimal buyVolUsd;
    private BigDecimal sellVolUsd;
    private BigDecimal pnlScore;
    private BigDecimal consistencyScore;
    private BigDecimal convictionScore;
    private BigDecimal totalScore;
    private String tier;
    private LocalDateTime lastTradeAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getSpecificTier(BigDecimal score) {
        if (score == null)
            return "C";
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0)
            return "S";
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0)
            return "A";
        if (score.compareTo(BigDecimal.valueOf(40)) >= 0)
            return "B";
        return "C";
    }

    public String getPnlTier() {
        return getSpecificTier(pnlScore);
    }

    public String getConsistencyTier() {
        return getSpecificTier(consistencyScore);
    }

    public String getConvictionTier() {
        return getSpecificTier(convictionScore);
    }
}
