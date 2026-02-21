package com.argus.job.config;

import com.argus.job.BlockMonitorJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration for scheduling jobs using Spring's @Scheduled annotation.
 * This is simpler than Quartz and perfect for our use case.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobSchedulerConfig {

    private final BlockMonitorJob blockMonitorJob;

    @Value("${argus.scheduler.block-monitor.interval-seconds:12}")
    private int blockMonitorIntervalSeconds;

    @Value("${argus.scheduler.block-monitor.enabled:true}")
    private boolean blockMonitorEnabled;

    /**
     * Monitors blockchain for new blocks at a fixed interval.
     * Runs every N seconds (default: 12 seconds) as configured in
     * application.properties.
     * 
     * Uses fixedDelayString to ensure the delay is between the END of one execution
     * and the START of the next, preventing overlapping executions.
     */
    @Scheduled(fixedDelayString = "${argus.scheduler.block-monitor.interval-seconds:12}000")
    public void monitorBlocks() {
        if (!blockMonitorEnabled) {
            log.info("Block monitor is disabled");
            return;
        }
        log.debug("Executing scheduled block monitoring job");
        try {
            blockMonitorJob.execute();
        } catch (Exception e) {
            log.error("Error executing block monitor job", e);
        }
    }
}
