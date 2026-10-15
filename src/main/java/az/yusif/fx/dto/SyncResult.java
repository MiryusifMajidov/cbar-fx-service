package az.yusif.fx.dto;

import java.time.LocalDate;

public record SyncResult(LocalDate requestedDate, LocalDate rateDate, int ratesCount) {
}
