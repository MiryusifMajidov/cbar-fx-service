package az.yusif.fx.scheduler;

import az.yusif.fx.exception.RatesNotPublishedException;
import az.yusif.fx.service.RateSyncService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "fx.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateSyncJob {

    private static final Logger log = LoggerFactory.getLogger(RateSyncJob.class);

    private final RateSyncService syncService;

    public RateSyncJob(RateSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(cron = "${fx.sync.cron}", zone = "${fx.sync.zone}")
    @SchedulerLock(name = "cbar-daily-sync", lockAtMostFor = "5m", lockAtLeastFor = "30s")
    public void syncToday() {
        try {
            syncService.sync(null);
        } catch (RatesNotPublishedException e) {
            log.info("Skipping scheduled sync: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Scheduled CBAR sync failed", e);
        }
    }
}
