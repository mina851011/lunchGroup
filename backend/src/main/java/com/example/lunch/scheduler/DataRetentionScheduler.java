package com.example.lunch.scheduler;

import com.example.lunch.service.DataRetentionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataRetentionScheduler {

    private final DataRetentionService dataRetentionService;

    @Value("${data.retention.enabled:true}")
    private boolean retentionEnabled;

    public DataRetentionScheduler(DataRetentionService dataRetentionService) {
        this.dataRetentionService = dataRetentionService;
    }

    @Scheduled(cron = "${data.retention.cron:0 30 3 * * *}", zone = "Asia/Taipei")
    public void cleanupOldData() {
        if (!retentionEnabled) {
            return;
        }

        try {
            dataRetentionService.cleanupAllRegions();
        } catch (Exception e) {
            log.error("[RETENTION] Scheduled cleanup failed: {}", e.getMessage(), e);
        }
    }
}
