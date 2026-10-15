package az.yusif.fx;

import az.yusif.fx.client.xml.ValCurs;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public final class CbarFixtures {

    public static final String SAMPLE = "cbar/15.10.2026.xml";

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private CbarFixtures() {
    }

    public static ValCurs load(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return XML_MAPPER.readValue(in, ValCurs.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
