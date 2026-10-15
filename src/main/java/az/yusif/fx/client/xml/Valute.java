package az.yusif.fx.client.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Valute(
        @JacksonXmlProperty(isAttribute = true, localName = "Code") String code,
        @JacksonXmlProperty(localName = "Nominal") String nominal,
        @JacksonXmlProperty(localName = "Name") String name,
        @JacksonXmlProperty(localName = "Value") BigDecimal value) {

    public int nominalAsInt() {
        if (nominal == null) {
            return 1;
        }
        String digits = nominal.replaceAll("\\D", "");
        return digits.isEmpty() ? 1 : Integer.parseInt(digits);
    }
}
