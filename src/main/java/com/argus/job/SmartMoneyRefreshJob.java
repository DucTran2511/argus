package com.argus.job;

import com.argus.domain.service.SmartMoneyScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartMoneyRefreshJob {

    private final SmartMoneyScoringService scoringService;

    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void runDailyRefresh() {
        log.info("Starting daily Smart Money metrics refresh job...");
        Instant start = Instant.now();

        try {
            int updatedCount = scoringService.refreshAllMetrics();

            Duration duration = Duration.between(start, Instant.now());
            log.info("Completed Smart Money metrics refresh. Total updated: {}, Time taken: {}s",
                    updatedCount, duration.toSeconds());
        } catch (Exception e) {
            log.error("Error occurred during daily Smart Money metrics refresh job", e);
        }
    }
}
