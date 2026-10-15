package az.yusif.fx.client.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ValType(
        @JacksonXmlProperty(isAttribute = true, localName = "Type") String type,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "Valute") List<Valute> valutes) {

    public boolean isMetal() {
        return type != null && type.toLowerCase(Locale.ROOT).contains("metal");
    }
}
