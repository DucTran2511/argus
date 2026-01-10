package com.argus.infra.blockchain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlchemyTransferDto {
    private String blockNum;
    private String hash;
    private String from;
    private String to;
    private String value;
    private String asset;
    private String category;
    private String uniqueId;
    private Integer logIndex;
    private RawContract rawContract;
    private Metadata metadata;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RawContract {
        private String address;
        private Integer decimals;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        private String blockTimestamp;
    }
}