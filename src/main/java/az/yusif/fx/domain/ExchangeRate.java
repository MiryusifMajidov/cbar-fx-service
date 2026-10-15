package az.yusif.fx.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record ExchangeRate(String currencyCode, LocalDate rateDate, int nominal, BigDecimal rate) {

    public BigDecimal unitRate() {
        if (nominal == 1) {
            return rate;
        }
        return rate.divide(BigDecimal.valueOf(nominal), 8, RoundingMode.HALF_UP);
    }
}
