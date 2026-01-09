package com.argus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DecodedSwap {

    private String functionName;
    private String dexRouter;
    private String tokenIn;
    private String tokenOut;
    private BigDecimal amountIn;
    private BigDecimal amountOut;
    private BigDecimal amountInMax;
    private BigDecimal amountOutMin;
    private List<String> path;
    private String recipient;
    private Long deadline;

    public boolean isExactIn() {
        return amountIn != null && amountOut == null;
    }

    public boolean isExactOut() {
        return amountOut != null && amountIn == null;
    }

    public boolean involvesETH() {
        String weth = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2";
        return (tokenIn != null && tokenIn.equalsIgnoreCase(weth)) ||
                (tokenOut != null && tokenOut.equalsIgnoreCase(weth));
    }

    public int getHopCount() {
        return path != null ? path.size() - 1 : 1;
    }
}
