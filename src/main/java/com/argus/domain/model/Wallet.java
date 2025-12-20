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
public class Wallet {

    private UUID id;
    private String address;
    private String chain;
    private String label;
    private WalletType type;
    private BigDecimal totalPnl;
    private BigDecimal winRate;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum WalletType {
        WHALE,
        VC,
        DEX_TRADER,
        INFLUENCER,
        SMART_MONEY,
        UNKNOWN
    }

    public boolean isWhale() {
        return type == WalletType.WHALE;
    }

    public boolean isProfitable() {
        return totalPnl != null && totalPnl.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isHighPerformer() {
        return isProfitable() &&
                winRate != null &&
                winRate.compareTo(BigDecimal.valueOf(0.6)) > 0;
    }

    public void updateActivity(LocalDateTime timestamp) {
        this.lastActivityAt = timestamp;
    }

    public void updatePnL(BigDecimal newPnl) {
        this.totalPnl = newPnl;
    }

    public void updateWinRate(BigDecimal newWinRate) {
        this.winRate = newWinRate;
    }
}
