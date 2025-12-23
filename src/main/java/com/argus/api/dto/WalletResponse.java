package com.argus.api.dto;

import com.argus.domain.model.Wallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {

    private UUID id;
    private String address;
    private String chain;
    private String label;
    private String type;
    private BigDecimal totalPnl;
    private BigDecimal winRate;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WalletResponse fromDomain(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .address(wallet.getAddress())
                .chain(wallet.getChain())
                .label(wallet.getLabel())
                .type(wallet.getType() != null ? wallet.getType().name() : null)
                .totalPnl(wallet.getTotalPnl())
                .winRate(wallet.getWinRate())
                .firstSeenAt(wallet.getFirstSeenAt())
                .lastActivityAt(wallet.getLastActivityAt())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
