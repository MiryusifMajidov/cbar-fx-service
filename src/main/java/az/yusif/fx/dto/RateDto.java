package az.yusif.fx.dto;

import az.yusif.fx.domain.CurrencyType;

import java.math.BigDecimal;

public record RateDto(String code, String name, int nominal, CurrencyType type, BigDecimal rate) {
}
