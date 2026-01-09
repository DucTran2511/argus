package com.argus.api.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class WalletTimelineResponse {
    private String address;
    private long totalTransactions;
    private int showing;
    private List<AssetTransferResponse> transactions;
}
