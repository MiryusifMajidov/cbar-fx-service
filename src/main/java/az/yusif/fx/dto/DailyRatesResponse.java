package az.yusif.fx.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyRatesResponse(LocalDate date, List<RateDto> rates) {
}
