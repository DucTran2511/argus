package com.argus.infra.blockchain.decoder;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.generated.Uint256;

import java.util.Arrays;
import java.util.List;

/**
 * Uniswap V2 Router function definitions.
 * 
 * Performance optimizations:
 * - Selectors stored in lowercase (avoids repeated toLowerCase() calls)
 * - No unused ParameterTypes class (Web3j needs TypeReference, not String[])
 * 
 * Reference: Uniswap V2 Router02
 * Address: 0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D
 */
public class RouterDefinitions {

    public static final String UNISWAP_V2_ROUTER = "0x7a250d5630b4cf539739df2c5dacb4c659f2488d";
    public static final String WETH = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2";

    /**
     * Function selectors in LOWERCASE (avoid heap allocation in hot path).
     */
    public static class FunctionSelectors {
        public static final String SWAP_EXACT_TOKENS_FOR_TOKENS = "0x38ed1739";
        public static final String SWAP_TOKENS_FOR_EXACT_TOKENS = "0x8803dbee";
        public static final String SWAP_EXACT_ETH_FOR_TOKENS = "0x7ff36ab5";
        public static final String SWAP_TOKENS_FOR_EXACT_ETH = "0x4a25d94a";
        public static final String SWAP_EXACT_TOKENS_FOR_ETH = "0x18cbafe5";
        public static final String SWAP_ETH_FOR_EXACT_TOKENS = "0xfb3bdb41";
    }

    /**
     * STATIC TYPE DEFINITIONS
     * Prevents creating thousands of temporary objects during high-frequency
     * scanning.
     */
    public static class ParameterTypes {
        private static final TypeReference<Uint256> UINT = new TypeReference<>() {
        };
        private static final TypeReference<Address> ADDR = new TypeReference<>() {
        };
        private static final TypeReference<DynamicArray<Address>> PATH = new TypeReference<>() {
        };

        // 1. Standard V2: (uint amountIn, uint amountOutMin, address[] path, address
        // to, uint deadline)
        public static final List<TypeReference<?>> SWAP_V2_STANDARD = Arrays.asList(UINT, UINT, PATH, ADDR, UINT);

        // 2. ETH Swaps: (uint amountOutMin, address[] path, address to, uint deadline)
        // Note: amountIn is missing here because it comes from tx.value
        public static final List<TypeReference<?>> SWAP_ETH_TO_TOKEN = Arrays.asList(UINT, PATH, ADDR, UINT);
    }

    public static boolean isKnownRouter(String address) {
        if (address == null)
            return false;
        return address.equalsIgnoreCase(UNISWAP_V2_ROUTER);
    }

    public static boolean isSwapFunction(String selector) {
        if (selector == null || selector.length() < 10)
            return false;

        // Single toLowerCase() call, compare against pre-lowercased constants
        String normalized = selector.toLowerCase();

        return normalized.equals(FunctionSelectors.SWAP_EXACT_TOKENS_FOR_TOKENS) ||
                normalized.equals(FunctionSelectors.SWAP_TOKENS_FOR_EXACT_TOKENS) ||
                normalized.equals(FunctionSelectors.SWAP_EXACT_ETH_FOR_TOKENS) ||
                normalized.equals(FunctionSelectors.SWAP_TOKENS_FOR_EXACT_ETH) ||
                normalized.equals(FunctionSelectors.SWAP_EXACT_TOKENS_FOR_ETH) ||
                normalized.equals(FunctionSelectors.SWAP_ETH_FOR_EXACT_TOKENS);
    }

    public static String getFunctionName(String selector) {
        if (selector == null)
            return "unknown";

        String normalized = selector.toLowerCase();

        if (normalized.equals(FunctionSelectors.SWAP_EXACT_TOKENS_FOR_TOKENS)) {
            return "swapExactTokensForTokens";
        } else if (normalized.equals(FunctionSelectors.SWAP_TOKENS_FOR_EXACT_TOKENS)) {
            return "swapTokensForExactTokens";
        } else if (normalized.equals(FunctionSelectors.SWAP_EXACT_ETH_FOR_TOKENS)) {
            return "swapExactETHForTokens";
        } else if (normalized.equals(FunctionSelectors.SWAP_TOKENS_FOR_EXACT_ETH)) {
            return "swapTokensForExactETH";
        } else if (normalized.equals(FunctionSelectors.SWAP_EXACT_TOKENS_FOR_ETH)) {
            return "swapExactTokensForETH";
        } else if (normalized.equals(FunctionSelectors.SWAP_ETH_FOR_EXACT_TOKENS)) {
            return "swapETHForExactTokens";
        }

        return "unknown";
    }
}