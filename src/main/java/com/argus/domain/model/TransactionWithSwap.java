package com.argus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionWithSwap {

    private Transaction transaction;
    private DecodedSwap decodedSwap;

    public boolean isSwap() {
        return decodedSwap != null;
    }
}
