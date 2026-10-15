package az.yusif.fx.service;

import az.yusif.fx.CbarFixtures;
import az.yusif.fx.client.CbarClient;
import az.yusif.fx.config.FxProperties;
import az.yusif.fx.domain.Currency;
import az.yusif.fx.domain.CurrencyType;
import az.yusif.fx.domain.ExchangeRate;
import az.yusif.fx.domain.SyncStatus;
import az.yusif.fx.dto.BackfillResult;
import az.yusif.fx.dto.SyncResult;
import az.yusif.fx.exception.InvalidRequestException;
import az.yusif.fx.exception.RatesNotPublishedException;
import az.yusif.fx.repository.CurrencyRepository;
import az.yusif.fx.repository.RateRepository;
import az.yusif.fx.repository.SyncLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateSyncServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Mock
    private CbarClient cbarClient;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private RateRepository rateRepository;
    @Mock
    private SyncLogRepository syncLogRepository;

    @Captor
    private ArgumentCaptor<Collection<Currency>> currencies;
    @Captor
    private ArgumentCaptor<Collection<ExchangeRate>> rates;

    private RateSyncService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T08:00:00Z"), ZoneId.of("Asia/Baku"));
        FxProperties properties = new FxProperties(
                new FxProperties.Cbar("https://www.cbar.az", Duration.ofSeconds(1), Duration.ofSeconds(1)),
                new FxProperties.Sync(true, "0 0 * * * *", "Asia/Baku", 31),
                new FxProperties.Security("admin", 60));
        TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));

        service = new RateSyncService(cbarClient, currencyRepository, rateRepository, syncLogRepository,
                transactionTemplate, clock, properties);
    }

    @Test
    void storesCurrenciesAndRatesForToday() {
        when(syncLogRepository.start(TODAY)).thenReturn(7L);
        when(cbarClient.fetchRates(TODAY)).thenReturn(CbarFixtures.load(CbarFixtures.SAMPLE));

        SyncResult result = service.sync(null);

        assertThat(result.rateDate()).isEqualTo(TODAY);
        assertThat(result.ratesCount()).isEqualTo(6);

        verify(currencyRepository).upsertAll(currencies.capture());
        assertThat(currencies.getValue())
                .extracting(Currency::code, Currency::name, Currency::type)
                .contains(
                        tuple("XAU", "Qızıl", CurrencyType.METAL),
                        tuple("USD", "ABŞ dolları", CurrencyType.FOREIGN),
                        tuple("JPY", "Yaponiya yeni", CurrencyType.FOREIGN));

        verify(rateRepository).upsertAll(rates.capture());
        assertThat(rates.getValue())
                .filteredOn(rate -> rate.currencyCode().equals("JPY"))
                .singleElement()
                .satisfies(jpy -> {
                    assertThat(jpy.nominal()).isEqualTo(100);
                    assertThat(jpy.rateDate()).isEqualTo(TODAY);
                });

        verify(syncLogRepository).finish(7L, SyncStatus.SUCCESS, 6, null);
    }

    @Test
    void recordsNotPublishedStatus() {
        LocalDate tomorrow = TODAY.plusDays(1);
        when(syncLogRepository.start(tomorrow)).thenReturn(8L);
        when(cbarClient.fetchRates(tomorrow)).thenThrow(new RatesNotPublishedException(tomorrow));

        assertThatThrownBy(() -> service.sync(tomorrow)).isInstanceOf(RatesNotPublishedException.class);

        verify(syncLogRepository).finish(eq(8L), eq(SyncStatus.NOT_PUBLISHED), eq(0), anyString());
        verifyNoInteractions(rateRepository);
    }

    @Test
    void recordsFailureWhenCbarIsUnreachable() {
        when(syncLogRepository.start(TODAY)).thenReturn(9L);
        when(cbarClient.fetchRates(TODAY)).thenThrow(new ResourceAccessException("Connection timed out"));

        assertThatThrownBy(() -> service.sync(TODAY)).isInstanceOf(ResourceAccessException.class);

        verify(syncLogRepository).finish(9L, SyncStatus.FAILED, 0, "Connection timed out");
        verify(rateRepository, never()).upsertAll(any());
    }

    @Test
    void backfillSeparatesMissingAndFailedDays() {
        LocalDate first = TODAY.minusDays(2);
        LocalDate second = TODAY.minusDays(1);

        when(syncLogRepository.start(any())).thenReturn(1L);
        when(cbarClient.fetchRates(first)).thenReturn(CbarFixtures.load(CbarFixtures.SAMPLE));
        when(cbarClient.fetchRates(second)).thenThrow(new RatesNotPublishedException(second));
        when(cbarClient.fetchRates(TODAY)).thenThrow(new ResourceAccessException("timeout"));

        BackfillResult result = service.backfill(first, TODAY);

        assertThat(result.synced()).isEqualTo(1);
        assertThat(result.notPublished()).containsExactly(second);
        assertThat(result.failed()).containsExactly(TODAY);
    }

    @Test
    void backfillRejectsRangesLongerThanLimit() {
        assertThatThrownBy(() -> service.backfill(TODAY.minusDays(40), TODAY))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(cbarClient, syncLogRepository);
    }
}
