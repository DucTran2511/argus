package com.argus.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_transfers", indexes = {
        @Index(name = "idx_wallet_address", columnList = "wallet_address"),
        @Index(name = "idx_tx_timestamp", columnList = "tx_timestamp"),
        @Index(name = "idx_tx_hash", columnList = "tx_hash")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetTransferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_address", nullable = false, length = 42)
    private String walletAddress;

    @Column(name = "tx_hash", nullable = false, length = 66)
    private String txHash;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "from_address", nullable = false, length = 42)
    private String fromAddress;

    @Column(name = "to_address", length = 42)
    private String toAddress;

    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "value", precision = 38, scale = 18)
    private BigDecimal value;

    @Column(name = "asset_symbol", length = 20)
    private String assetSymbol;

    @Column(name = "token_address", length = 42)
    private String tokenAddress;

    @Column(name = "tx_timestamp")
    private LocalDateTime txTimestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "usd_value", precision = 38, scale = 18)
    private BigDecimal usdValue;
}
