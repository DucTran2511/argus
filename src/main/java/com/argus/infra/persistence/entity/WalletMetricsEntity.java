package com.argus.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletMetricsEntity {

    @Id
    @Column(name = "wallet_address", length = 66)
    private String walletAddress;

    @Column(name = "archetype", length = 50)
    @Enumerated(EnumType.STRING)
    private Archetype archetype;

    @Column(name = "is_blacklisted")
    private Boolean isBlacklisted;

    @Column(name = "avg_position_size_usd", precision = 20, scale = 2)
    private BigDecimal avgPositionSizeUsd;

    @Column(name = "trade_frequency_per_month", precision = 10, scale = 2)
    private BigDecimal tradeFrequencyPerMonth;

    @Column(name = "trade_count_7d")
    private Integer tradeCount7d;

    @Column(name = "max_roi_percent", precision = 10, scale = 2)
    private BigDecimal maxRoiPercent;

    @Column(name = "profit_factor", precision = 10, scale = 4)
    private BigDecimal profitFactor;

    @Column(name = "avg_hold_time_sec")
    private Integer avgHoldTimeSec;

    @Column(name = "buy_vol_usd", precision = 20, scale = 2)
    private BigDecimal buyVolUsd;

    @Column(name = "sell_vol_usd", precision = 20, scale = 2)
    private BigDecimal sellVolUsd;

    @Column(name = "pnl_score", precision = 5, scale = 2)
    private BigDecimal pnlScore;

    @Column(name = "consistency_score", precision = 5, scale = 2)
    private BigDecimal consistencyScore;

    @Column(name = "conviction_score", precision = 5, scale = 2)
    private BigDecimal convictionScore;

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "tier", length = 2)
    private String tier;

    @Column(name = "last_trade_at")
    private LocalDateTime lastTradeAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Archetype {
        SNIPER,
        HOME_RUN,
        WHALE,
        ACCUMULATOR,
        MEV_BOT,
        UNKNOWN
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
