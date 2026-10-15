package az.yusif.fx.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyChangesResponse(LocalDate date, List<RateChangeDto> changes) {
}
