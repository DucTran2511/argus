package com.argus.infra.blockchain.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class AlchemyAssetTransfersRequest {
    private String fromBlock;
    private String toBlock;
    private String fromAddress;
    private String toAddress;
    private List<String> categories;
    private String maxCount;
    private Boolean withMetadata;
    private String order;
    private String pageKey;
}
