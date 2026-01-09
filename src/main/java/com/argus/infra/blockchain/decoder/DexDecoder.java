package com.argus.infra.blockchain.decoder;

import com.argus.domain.model.DecodedSwap;
import com.argus.domain.port.blockchain.DexDecoderPort;
import com.argus.infra.blockchain.decoder.RouterDefinitions.FunctionSelectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Slf4j
@Component
public class DexDecoder implements DexDecoderPort {

        /**
         * Main entry point to detect and decode a swap.
         * 
         * @param to      The target address (Router)
         * @param input   The hex input data
         * @param txValue The value of ETH sent (CRITICAL for ETH swaps)
         */
        public DecodedSwap decodeSwap(String to, String input, BigDecimal txValue) {
                // 1. Fast Filter
                if (!RouterDefinitions.isKnownRouter(to) || input == null || input.length() < 10) {
                        return null;
                }

                String selector = input.substring(0, 10).toLowerCase();

                try {
                        // 2. Efficient Dispatch
                        switch (selector) {
                                // --- Case A: Standard Token Swaps ---
                                case FunctionSelectors.SWAP_EXACT_TOKENS_FOR_TOKENS:
                                        return decodeStandardV2(input, "swapExactTokensForTokens", to, false);
                                case FunctionSelectors.SWAP_TOKENS_FOR_EXACT_TOKENS:
                                        return decodeStandardV2(input, "swapTokensForExactTokens", to, true);

                                // --- Case B: ETH -> Token (AmountIn is in txValue) ---
                                case FunctionSelectors.SWAP_EXACT_ETH_FOR_TOKENS:
                                        return decodeEthToToken(input, "swapExactETHForTokens", to, txValue, false);
                                case FunctionSelectors.SWAP_ETH_FOR_EXACT_TOKENS:
                                        return decodeEthToToken(input, "swapETHForExactTokens", to, txValue, true);

                                // --- Case C: Token -> ETH ---
                                case FunctionSelectors.SWAP_EXACT_TOKENS_FOR_ETH:
                                        return decodeStandardV2(input, "swapExactTokensForETH", to, false);
                                case FunctionSelectors.SWAP_TOKENS_FOR_EXACT_ETH:
                                        return decodeStandardV2(input, "swapTokensForExactETH", to, true);

                                default:
                                        return null;
                        }
                } catch (Exception e) {
                        log.error("Error decoding swap {}: {}", selector, e.getMessage());
                        return null;
                }
        }

        // --- Core Logic ---

        private DecodedSwap decodeStandardV2(String input, String funcName, String router, boolean isExactOutput) {
                // FAST: Uses static definition
                List<Type> decoded = decodeRaw(input, RouterDefinitions.ParameterTypes.SWAP_V2_STANDARD);

                BigInteger val1 = (BigInteger) decoded.get(0).getValue(); // AmountIn or AmountOut
                BigInteger val2 = (BigInteger) decoded.get(1).getValue(); // AmountOutMin or AmountInMax
                List<String> path = extractPath(decoded.get(2));
                String recipient = decoded.get(3).getValue().toString();
                long deadline = ((BigInteger) decoded.get(4).getValue()).longValue();

                return DecodedSwap.builder()
                                .functionName(funcName)
                                .dexRouter(router)
                                .tokenIn(path.getFirst())
                                .tokenOut(path.getLast())
                                .path(path)
                                .recipient(recipient)
                                .deadline(deadline)
                                .amountIn(isExactOutput ? null : new BigDecimal(val1))
                                .amountInMax(isExactOutput ? new BigDecimal(val2) : null)
                                .amountOut(isExactOutput ? new BigDecimal(val1) : null)
                                .amountOutMin(isExactOutput ? null : new BigDecimal(val2))
                                .build();
        }

        private DecodedSwap decodeEthToToken(String input, String funcName, String router, BigDecimal txValue,
                        boolean isExactOutput) {
                // FAST: Uses static definition
                List<Type> decoded = decodeRaw(input, RouterDefinitions.ParameterTypes.SWAP_ETH_TO_TOKEN);

                BigInteger val1 = (BigInteger) decoded.get(0).getValue(); // amountOutMin or amountOut
                List<String> path = extractPath(decoded.get(1));
                String recipient = decoded.get(2).toString();
                long deadline = ((BigInteger) decoded.get(3).getValue()).longValue();

                return DecodedSwap.builder()
                                .functionName(funcName)
                                .dexRouter(router)
                                .tokenIn(RouterDefinitions.WETH)
                                .tokenOut(path.getLast())
                                .path(path)
                                .recipient(recipient)
                                .deadline(deadline)
                                // CAPTURE THE ETH VALUE HERE
                                .amountIn(isExactOutput ? null : txValue)
                                .amountInMax(isExactOutput ? txValue : null)
                                .amountOut(isExactOutput ? new BigDecimal(val1) : null)
                                .amountOutMin(isExactOutput ? null : new BigDecimal(val1))
                                .build();
        }

        // --- Low Level Helpers ---

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private List<Type> decodeRaw(String input, List<TypeReference<?>> types) {
                String payload = input.substring(10);
                return FunctionReturnDecoder.decode(payload, (List) types);
        }

        @SuppressWarnings("unchecked")
        private List<String> extractPath(Type pathType) {
                return ((List<Address>) pathType.getValue()).stream()
                                .map(Address::toString)
                                .toList();
        }
}