package az.yusif.fx.dto;

import java.time.LocalDate;
import java.util.List;

public record BackfillResult(LocalDate from,
                             LocalDate to,
                             int synced,
                             List<LocalDate> notPublished,
                             List<LocalDate> failed) {
}
