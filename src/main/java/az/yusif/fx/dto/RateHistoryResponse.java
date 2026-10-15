package az.yusif.fx.dto;

import java.time.LocalDate;
import java.util.List;

public record RateHistoryResponse(String code, LocalDate from, LocalDate to, List<RatePoint> rates) {
}
