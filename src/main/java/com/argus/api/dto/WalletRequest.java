package com.argus.api.dto;

import com.argus.domain.model.Wallet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletRequest {

    @NotBlank(message = "Wallet address is required")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid Ethereum address format")
    private String address;

    @NotBlank(message = "Chain is required")
    private String chain;

    private String label;

    private String type;

    private BigDecimal totalPnl;

    private BigDecimal winRate;

    public Wallet toDomain() {
        return Wallet.builder()
                .address(this.address)
                .chain(this.chain)
                .label(this.label)
                .type(this.type != null ? Wallet.WalletType.valueOf(this.type.toUpperCase())
                        : Wallet.WalletType.UNKNOWN)
                .totalPnl(this.totalPnl)
                .winRate(this.winRate)
                .build();
    }
}
