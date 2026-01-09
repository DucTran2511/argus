package com.argus.domain.port.blockchain;

import com.argus.domain.model.DecodedSwap;
import java.math.BigDecimal;

public interface DexDecoderPort {

    DecodedSwap decodeSwap(String to, String input, BigDecimal txValue);
}
