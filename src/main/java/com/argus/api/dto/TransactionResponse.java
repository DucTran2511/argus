package com.argus.api.dto;

import com.argus.domain.model.DecodedSwap;
import com.argus.domain.model.Transaction;
import com.argus.domain.model.TransactionWithSwap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {

    private Long id;
    private String txHash;
    private String from;
    private String to;
    private BigDecimal value;
    private String input;
    private String chain;
    private Long blockNumber;
    private Long gasUsed;
    private BigDecimal gasPrice;
    private DecodedSwap decodedSwap;

    public static TransactionResponse from(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .txHash(tx.getTxHash())
                .from(tx.getFrom())
                .to(tx.getTo())
                .value(tx.getValue())
                .input(tx.getInput())
                .chain(tx.getChain())
                .blockNumber(tx.getBlockNumber())
                .gasUsed(tx.getGasUsed())
                .gasPrice(tx.getGasPrice())
                .build();
    }

    public static TransactionResponse from(TransactionWithSwap txWithSwap) {
        Transaction tx = txWithSwap.getTransaction();
        return TransactionResponse.builder()
                .id(tx.getId())
                .txHash(tx.getTxHash())
                .from(tx.getFrom())
                .to(tx.getTo())
                .value(tx.getValue())
                .input(tx.getInput())
                .chain(tx.getChain())
                .blockNumber(tx.getBlockNumber())
                .gasUsed(tx.getGasUsed())
                .gasPrice(tx.getGasPrice())
                .decodedSwap(txWithSwap.getDecodedSwap())
                .build();
    }
}
