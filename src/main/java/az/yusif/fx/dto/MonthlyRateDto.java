package az.yusif.fx.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyRateDto(YearMonth month, BigDecimal average, BigDecimal min, BigDecimal max, int days) {
}
