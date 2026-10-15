package az.yusif.fx.repository;

import az.yusif.fx.domain.Currency;
import az.yusif.fx.domain.CurrencyType;
import az.yusif.fx.domain.ExchangeRate;
import az.yusif.fx.dto.MonthlyRateDto;
import az.yusif.fx.dto.RateChangeDto;
import az.yusif.fx.dto.RateStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({CurrencyRepository.class, RateRepository.class})
class RateRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private RateRepository rateRepository;

    @BeforeEach
    void seed() {
        currencyRepository.upsertAll(List.of(
                new Currency("USD", "ABŞ dolları", 1, CurrencyType.FOREIGN),
                new Currency("EUR", "Avro", 1, CurrencyType.FOREIGN)));

        rateRepository.upsertAll(List.of(
                rate("USD", "2026-09-30", "1.7000"),
                rate("USD", "2026-10-13", "1.7000"),
                rate("USD", "2026-10-14", "1.7000"),
                rate("USD", "2026-10-15", "1.7000"),
                rate("EUR", "2026-09-30", "1.9600"),
                rate("EUR", "2026-10-13", "1.9700"),
                rate("EUR", "2026-10-14", "1.9800"),
                rate("EUR", "2026-10-15", "1.9750")));
    }

    @Test
    void upsertReplacesExistingRate() {
        rateRepository.upsertAll(List.of(rate("EUR", "2026-10-15", "1.9900")));

        assertThat(rateRepository.findOnOrBefore("EUR", LocalDate.parse("2026-10-15")))
                .get()
                .extracting(ExchangeRate::rate)
                .satisfies(rate -> assertThat(rate).isEqualByComparingTo("1.99"));
    }

    @Test
    void findsLatestAvailableDate() {
        assertThat(rateRepository.findLatestDate(LocalDate.parse("2026-10-20"))).contains(LocalDate.parse("2026-10-15"));
        assertThat(rateRepository.findLatestDate(LocalDate.parse("2026-01-01"))).isEmpty();
    }

    @Test
    void calculatesPeriodStats() {
        RateStats stats = rateRepository.findStats("EUR", LocalDate.parse("2026-10-13"), LocalDate.parse("2026-10-15"))
                .orElseThrow();

        assertThat(stats.days()).isEqualTo(3);
        assertThat(stats.min()).isEqualByComparingTo("1.97");
        assertThat(stats.max()).isEqualByComparingTo("1.98");
        assertThat(stats.average()).isEqualByComparingTo("1.975");
        assertThat(stats.first()).isEqualByComparingTo("1.97");
        assertThat(stats.last()).isEqualByComparingTo("1.975");
        assertThat(stats.changePercent()).isEqualByComparingTo("0.2538");
    }

    @Test
    void statsAreEmptyForPeriodWithoutRates() {
        assertThat(rateRepository.findStats("EUR", LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31")))
                .isEmpty();
    }

    @Test
    void calculatesDayOverDayChanges() {
        List<RateChangeDto> changes = rateRepository.findChanges(LocalDate.parse("2026-10-15"));

        assertThat(changes).extracting(RateChangeDto::code).containsExactly("EUR", "USD");

        RateChangeDto eur = changes.get(0);
        assertThat(eur.previousRate()).isEqualByComparingTo("1.98");
        assertThat(eur.change()).isEqualByComparingTo("-0.005");
        assertThat(eur.changePercent()).isEqualByComparingTo("-0.2525");

        assertThat(changes.get(1).change()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void groupsRatesByMonth() {
        List<MonthlyRateDto> months = rateRepository.findMonthly("EUR", 2026);

        assertThat(months).extracting(MonthlyRateDto::month)
                .containsExactly(YearMonth.of(2026, 9), YearMonth.of(2026, 10));
        assertThat(months.get(1).days()).isEqualTo(3);
        assertThat(months.get(1).average()).isEqualByComparingTo("1.975");
    }

    private static ExchangeRate rate(String code, String date, String value) {
        return new ExchangeRate(code, LocalDate.parse(date), 1, new BigDecimal(value));
    }
}
