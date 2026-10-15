package az.yusif.fx.client.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ValCurs(
        @JacksonXmlProperty(isAttribute = true, localName = "Date") String date,
        @JacksonXmlProperty(isAttribute = true, localName = "Name") String name,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "ValType") List<ValType> types) {
}
