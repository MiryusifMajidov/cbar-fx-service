package az.yusif.fx.client;

import az.yusif.fx.CbarFixtures;
import az.yusif.fx.client.xml.ValCurs;
import az.yusif.fx.client.xml.ValType;
import az.yusif.fx.client.xml.Valute;
import az.yusif.fx.exception.RatesNotPublishedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CbarClientTest {

    private MockRestServiceServer server;
    private CbarClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://www.cbar.az");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CbarClient(builder.build());
    }

    @Test
    void parsesDailyRatesXml() {
        server.expect(requestTo("https://www.cbar.az/currencies/15.10.2026.xml"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ClassPathResource(CbarFixtures.SAMPLE), MediaType.APPLICATION_XML));

        ValCurs valCurs = client.fetchRates(LocalDate.of(2026, 10, 15));

        assertThat(valCurs.date()).isEqualTo("15.10.2026");
        assertThat(valCurs.types()).hasSize(2);

        ValType metals = valCurs.types().get(0);
        assertThat(metals.isMetal()).isTrue();
        assertThat(metals.valutes()).extracting(Valute::code).containsExactly("XAU", "XAG");

        ValType foreign = valCurs.types().get(1);
        assertThat(foreign.isMetal()).isFalse();
        Valute jpy = foreign.valutes().stream()
                .filter(valute -> valute.code().equals("JPY"))
                .findFirst()
                .orElseThrow();
        assertThat(jpy.nominalAsInt()).isEqualTo(100);
        assertThat(jpy.value()).isEqualByComparingTo("1.1240");

        server.verify();
    }

    @Test
    void metalNominalIsParsedAsOne() {
        Valute gold = CbarFixtures.load(CbarFixtures.SAMPLE).types().get(0).valutes().get(0);

        assertThat(gold.nominal()).isEqualTo("1 t.u.");
        assertThat(gold.nominalAsInt()).isEqualTo(1);
    }

    @Test
    void notFoundMeansRatesAreNotPublishedYet() {
        server.expect(requestTo("https://www.cbar.az/currencies/16.10.2026.xml"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.fetchRates(LocalDate.of(2026, 10, 16)))
                .isInstanceOf(RatesNotPublishedException.class)
                .hasMessageContaining("2026-10-16");
    }
}
