package az.yusif.fx.domain;

public record Currency(String code, String name, int nominal, CurrencyType type) {

    public static final String AZN = "AZN";
}
