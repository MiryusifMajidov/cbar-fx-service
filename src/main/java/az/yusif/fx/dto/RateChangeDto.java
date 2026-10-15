package az.yusif.fx.dto;

import java.math.BigDecimal;

public record RateChangeDto(String code,
                            String name,
                            int nominal,
                            BigDecimal rate,
                            BigDecimal previousRate,
                            BigDecimal change,
                            BigDecimal changePercent) {
}
