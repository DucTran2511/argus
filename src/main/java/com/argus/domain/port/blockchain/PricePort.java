package com.argus.domain.port.blockchain;

import com.argus.domain.model.TokenPrice;
import com.argus.domain.model.TokenPriceRange;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PricePort {
    Optional<TokenPrice> getTokenPrice(String tokenAddress, String chain);

    BigDecimal getEthPrice();

    Optional<TokenPriceRange> getTokenPriceRange(String tokenAddress, LocalDateTime from, LocalDateTime to);
}
