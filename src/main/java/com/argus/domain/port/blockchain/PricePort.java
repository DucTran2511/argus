package com.argus.domain.port.blockchain;

import com.argus.domain.model.TokenPrice;

import java.math.BigDecimal;
import java.util.Optional;

public interface PricePort {
    Optional<TokenPrice> getTokenPrice(String tokenAddress, String chain);

    BigDecimal getEthPrice();
}
