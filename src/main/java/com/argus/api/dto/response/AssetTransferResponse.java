package com.argus.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.Builder;
import com.argus.domain.model.AssetTransfer;

@Data
@Builder
public class AssetTransferResponse {
    private String txHash;
    private Long blockNumber;
    private String from;
    private String to;
    private String category;
    private BigDecimal value;
    private String assetSymbol;
    private String tokenAddress;
    private LocalDateTime timestamp;
    private String direction; // "SENT" or "RECEIVED"
    private String etherscanUrl;

    public static AssetTransferResponse fromDomain(AssetTransfer transfer) {
        return AssetTransferResponse.builder()
                .txHash(transfer.getTxHash())
                .blockNumber(transfer.getBlockNumber())
                .from(transfer.getFrom())
                .to(transfer.getTo())
                .category(transfer.getCategory().name())
                .value(transfer.getValue())
                .assetSymbol(transfer.getAssetSymbol())
                .tokenAddress(transfer.getTokenAddress())
                .timestamp(transfer.getTxTimestamp())
                .direction(determineDirection(transfer))
                .etherscanUrl("https://etherscan.io/tx/" + transfer.getTxHash())
                .build();
    }

    private static String determineDirection(AssetTransfer transfer) {
        return transfer.getFrom().equalsIgnoreCase(transfer.getWalletAddress())
                ? "SENT"
                : "RECEIVED";
    }
}
