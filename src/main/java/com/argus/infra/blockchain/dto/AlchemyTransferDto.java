package com.argus.infra.blockchain.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AlchemyTransferDto {
    private String blockNum;
    private String hash;
    private String from;
    private String to;
    private BigDecimal value;
    private String asset;
    private String category;
    private String uniqueId;
    private Integer logIndex;
    private RawContract rawContract;
    private Metadata metadata;

    @Data
    public static class RawContract {
        private String address;
        private Integer decimals;
    }

    @Data
    public static class Metadata {
        private String blockTimestamp;
    }
}