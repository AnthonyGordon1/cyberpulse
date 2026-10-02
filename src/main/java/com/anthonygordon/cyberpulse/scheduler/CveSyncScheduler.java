package com.anthonygordon.cyberpulse.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.anthonygordon.cyberpulse.service.CveService;

@Component
@ConditionalOnProperty(name = "cyberpulse.sync.enabled", havingValue = "true", matchIfMissing = true)
public class CveSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(CveSyncScheduler.class);
    private final CveService cveService;

    public CveSyncScheduler(CveService cveService) {
        this.cveService = cveService;
    }

    @Scheduled(initialDelayString = "${cyberpulse.sync.initial-delay}", fixedDelayString = "${cyberpulse.sync.interval}")
    public void runScheduledSync() {
        try {
            int saved = cveService.syncRecentCves();
            log.info("Scheduled CVE sync finished, saved {} CVEs", saved);
        } catch (Exception e) {
            log.error("Scheduled CVE sync failed, will retry on next run", e);
        }
    }

}
