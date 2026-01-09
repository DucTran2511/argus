package com.argus.infra.blockchain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Represents an ERC20 Transfer event decoded from transaction logs.
 * 
 * Event signature: Transfer(address indexed from, address indexed to, uint256
 * value)
 * Topic0 (event signature hash):
 * 0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Erc20TransferEvent {

    private String tokenAddress; // Contract address that emitted the event
    private String from; // Sender address (indexed topic1)
    private String to; // Recipient address (indexed topic2)
    private BigDecimal amount; // Amount transferred (from data, in token's smallest unit)
    private int decimals; // Token decimals (default 18 if unknown)
    private String txHash; // Transaction hash
    private Long blockNumber; // Block number
    private Integer logIndex; // Log index in the transaction

    /**
     * Get the amount in human-readable format (divided by 10^decimals)
     */
    public BigDecimal getAmountFormatted() {
        if (amount == null)
            return BigDecimal.ZERO;
        BigDecimal divisor = BigDecimal.TEN.pow(decimals);
        return amount.divide(divisor, decimals, java.math.RoundingMode.HALF_UP);
    }
}
