package com.argus.api.dto.response;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class SyncResponse {
    private String address;
    private int syncedCount;
    private String message;
}