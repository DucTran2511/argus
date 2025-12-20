package com.argus.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "signals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "wallet_id")
    private UUID walletId;

    @Column(name = "token_address", length = 66)
    private String tokenAddress;

    @Column(name = "token_symbol", length = 20)
    private String tokenSymbol;

    @Column(name = "chain", nullable = false, length = 20)
    private String chain;

    @Column(name = "usd_value", precision = 20, scale = 2)
    private BigDecimal usdValue;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "ai_narrative")
    private String aiNarrative;

    @Column(name = "metadata")
    private String metadata;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    private void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
