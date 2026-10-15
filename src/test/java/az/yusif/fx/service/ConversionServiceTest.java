package az.yusif.fx.service;

import az.yusif.fx.domain.ExchangeRate;
import az.yusif.fx.dto.ConversionResponse;
import az.yusif.fx.exception.NotFoundException;
import az.yusif.fx.repository.RateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Mock
    private RateRepository rateRepository;

    private ConversionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T08:00:00Z"), ZoneId.of("Asia/Baku"));
        service = new ConversionService(rateRepository, clock);
    }

    @Test
    void convertsBetweenTwoForeignCurrencies() {
        stubRate("USD", 1, "1.7000");
        stubRate("EUR", 1, "1.9785");

        ConversionResponse response = service.convert("usd", "eur", new BigDecimal("100"), null);

        assertThat(response.from()).isEqualTo("USD");
        assertThat(response.to()).isEqualTo("EUR");
        assertThat(response.result()).isEqualByComparingTo("85.9237");
        assertThat(response.rate()).isEqualByComparingTo("0.859237");
        assertThat(response.rateDate()).isEqualTo(TODAY);
    }

    @Test
    void takesNominalIntoAccount() {
        stubRate("JPY", 100, "1.1240");

        ConversionResponse response = service.convert("AZN", "JPY", new BigDecimal("10"), TODAY);

        assertThat(response.result()).isEqualByComparingTo("889.6797");
    }

    @Test
    void convertsToManat() {
        stubRate("USD", 1, "1.7000");

        ConversionResponse response = service.convert("USD", "AZN", new BigDecimal("50"), TODAY);

        assertThat(response.result()).isEqualByComparingTo("85.0000");
        assertThat(response.rate()).isEqualByComparingTo("1.7");
    }

    @Test
    void failsWhenRateIsMissing() {
        when(rateRepository.findOnOrBefore("GBP", TODAY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.convert("GBP", "AZN", BigDecimal.ONE, TODAY))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("GBP");
    }

    private void stubRate(String code, int nominal, String rate) {
        when(rateRepository.findOnOrBefore(code, TODAY))
                .thenReturn(Optional.of(new ExchangeRate(code, TODAY, nominal, new BigDecimal(rate))));
    }
}
