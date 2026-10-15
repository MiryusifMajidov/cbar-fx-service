package az.yusif.fx.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RateStats(String code,
                        LocalDate from,
                        LocalDate to,
                        int days,
                        BigDecimal min,
                        BigDecimal max,
                        BigDecimal average,
                        BigDecimal first,
                        BigDecimal last,
                        BigDecimal changePercent) {
}
