package az.yusif.fx.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RatePoint(LocalDate date, int nominal, BigDecimal rate) {
}
