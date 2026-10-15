package az.yusif.fx.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConversionResponse(String from,
                                 String to,
                                 BigDecimal amount,
                                 BigDecimal result,
                                 BigDecimal rate,
                                 LocalDate rateDate) {
}
