package com.argus.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetTransfer {
    private Long id;
    private String walletAddress;
    private String txHash;
    private Long blockNumber;
    private String from;
    private String to;
    private TransferCategory category;
    private BigDecimal value; // Already decoded amount!
    private String assetSymbol; // "ETH", "USDC", "WETH"
    private String tokenAddress; // Contract address (null for ETH)
    private LocalDateTime txTimestamp;
    private LocalDateTime createdAt;
    private BigDecimal usdValue;

    public enum TransferCategory {
        EXTERNAL, // Standard ETH transfer
        INTERNAL, // Internal transaction (smart contract)
        ERC20, // Token transfer
        ERC721, // NFT transfer (future)
        ERC1155 // Multi-token (future)

    }
}
