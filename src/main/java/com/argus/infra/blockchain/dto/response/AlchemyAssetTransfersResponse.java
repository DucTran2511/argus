package com.argus.infra.blockchain.dto.response;

import lombok.Data;
import java.util.List;

import com.argus.infra.blockchain.dto.AlchemyTransferDto;

@Data
public class AlchemyAssetTransfersResponse {
    private List<AlchemyTransferDto> transfers;
    private String pageKey;
}