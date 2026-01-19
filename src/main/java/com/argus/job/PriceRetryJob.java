package com.argus.job;

import com.argus.domain.model.AssetTransfer;
import com.argus.domain.model.TokenPriceRange;
import com.argus.domain.port.blockchain.PricePort;
import com.argus.domain.port.persistence.TransactionPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceRetryJob {

    private final TransactionPersistencePort transactionPort;
    private final PricePort pricePort;

    @Scheduled(cron = "0 0 3 * * ?")
    public void retryMissingPrices() {
        log.info("Starting price retry job...");

        List<AssetTransfer> missing = transactionPort.findByPriceSourceIn(
                List.of("missing", "error"));

        if (missing.isEmpty()) {
            log.info("No missing prices to retry");
            return;
        }

        log.info("Found {} transfers with missing prices", missing.size());

        Map<String, List<AssetTransfer>> byToken = missing.stream()
                .filter(t -> t.getTokenAddress() != null)
                .collect(Collectors.groupingBy(AssetTransfer::getTokenAddress));

        int updated = 0;

        for (var entry : byToken.entrySet()) {
            String tokenAddr = entry.getKey();
            List<AssetTransfer> txs = entry.getValue();

            LocalDateTime minDate = txs.stream()
                    .map(AssetTransfer::getTxTimestamp)
                    .min(Comparator.naturalOrder())
                    .orElse(LocalDateTime.now().minusDays(30));
            LocalDateTime maxDate = txs.stream()
                    .map(AssetTransfer::getTxTimestamp)
                    .max(Comparator.naturalOrder())
                    .orElse(LocalDateTime.now());

            Optional<TokenPriceRange> rangeOpt = pricePort.getTokenPriceRange(
                    tokenAddr, minDate, maxDate);

            if (rangeOpt.isEmpty()) {
                log.warn("Still no price data for token: {}", tokenAddr);
                continue;
            }

            TokenPriceRange range = rangeOpt.get();

            for (AssetTransfer transfer : txs) {
                BigDecimal price = range.getPriceAtTimestamp(transfer.getTxTimestamp());
                if (price != null) {
                    transfer.setPriceAtTx(price);
                    transfer.setPriceSource("coingecko");
                    if (transfer.getValue() != null) {
                        transfer.setUsdValue(transfer.getValue().multiply(price));
                    }
                    updated++;
                }
            }

            transactionPort.saveAssetTransfers(txs);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Price retry job interrupted");
                return;
            }
        }

        log.info("Price retry complete: updated {} transfers", updated);
    }
}
