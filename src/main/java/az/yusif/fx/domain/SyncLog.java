package az.yusif.fx.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SyncLog(long id,
                      LocalDate requestedDate,
                      SyncStatus status,
                      Integer ratesCount,
                      String message,
                      OffsetDateTime startedAt,
                      OffsetDateTime finishedAt) {
}
