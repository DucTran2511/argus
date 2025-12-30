package com.argus.infra.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id")
    private UUID walletId;

    @Column(length = 66)
    private String txHash;

    @Column(length = 20)
    private String chain;

    @Column(length = 20)
    private String type;

    // Core transaction fields from blockchain
    @Column(name = "from_address", length = 42)
    private String from;

    @Column(name = "to_address", length = 42)
    private String to;

    @Column(name = "value_eth", precision = 30, scale = 18)
    private BigDecimal value;

    @Column(columnDefinition = "TEXT")
    private String input;

    // DEX-specific fields
    @Column(length = 66)
    private String tokenIn;

    @Column(length = 66)
    private String tokenOut;

    @Column(precision = 30, scale = 18)
    private BigDecimal amountIn;

    @Column(precision = 30, scale = 18)
    private BigDecimal amountOut;

    @Column(precision = 20, scale = 2)
    private BigDecimal usdValue;

    // Block metadata
    private Long blockNumber;

    private Long gasUsed;

    @Column(precision = 30, scale = 18)
    private BigDecimal gasPrice;

    private LocalDateTime txTimestamp;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
