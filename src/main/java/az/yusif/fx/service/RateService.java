package az.yusif.fx.service;

import az.yusif.fx.domain.ExchangeRate;
import az.yusif.fx.dto.DailyChangesResponse;
import az.yusif.fx.dto.DailyRatesResponse;
import az.yusif.fx.dto.MonthlyRateDto;
import az.yusif.fx.dto.RateHistoryResponse;
import az.yusif.fx.dto.RateStats;
import az.yusif.fx.exception.InvalidRequestException;
import az.yusif.fx.exception.NotFoundException;
import az.yusif.fx.repository.RateRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RateService {

    static final int MAX_RANGE_DAYS = 366;
    private static final int DEFAULT_HISTORY_DAYS = 30;

    private final RateRepository rateRepository;
    private final CurrencyService currencyService;
    private final Clock clock;

    public RateService(RateRepository rateRepository, CurrencyService currencyService, Clock clock) {
        this.rateRepository = rateRepository;
        this.currencyService = currencyService;
        this.clock = clock;
    }

    @Cacheable(cacheNames = "daily-rates", key = "#date ?: 'latest'")
    public DailyRatesResponse getDailyRates(@Nullable LocalDate date) {
        LocalDate effectiveDate = resolveDate(date);
        return new DailyRatesResponse(effectiveDate, rateRepository.findByDate(effectiveDate));
    }

    public ExchangeRate getLatest(String code) {
        String currency = currencyService.getByCode(code).code();
        return rateRepository.findOnOrBefore(currency, LocalDate.now(clock))
                .orElseThrow(() -> new NotFoundException("No rates stored for " + currency));
    }

    public RateHistoryResponse getHistory(String code, @Nullable LocalDate from, @Nullable LocalDate to) {
        String currency = currencyService.getByCode(code).code();
        LocalDate end = to != null ? to : LocalDate.now(clock);
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_HISTORY_DAYS);
        validateRange(start, end);
        return new RateHistoryResponse(currency, start, end, rateRepository.findHistory(currency, start, end));
    }

    public RateStats getStats(String code, LocalDate from, LocalDate to) {
        String currency = currencyService.getByCode(code).code();
        validateRange(from, to);
        return rateRepository.findStats(currency, from, to)
                .orElseThrow(() -> new NotFoundException("No %s rates between %s and %s".formatted(currency, from, to)));
    }

    public DailyChangesResponse getChanges(@Nullable LocalDate date) {
        LocalDate effectiveDate = resolveDate(date);
        return new DailyChangesResponse(effectiveDate, rateRepository.findChanges(effectiveDate));
    }

    public List<MonthlyRateDto> getMonthly(String code, @Nullable Integer year) {
        String currency = currencyService.getByCode(code).code();
        int targetYear = year != null ? year : LocalDate.now(clock).getYear();
        return rateRepository.findMonthly(currency, targetYear);
    }

    private LocalDate resolveDate(@Nullable LocalDate date) {
        LocalDate requested = date != null ? date : LocalDate.now(clock);
        return rateRepository.findLatestDate(requested)
                .orElseThrow(() -> new NotFoundException("No rates available on or before " + requested));
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new InvalidRequestException("Date range must not exceed " + MAX_RANGE_DAYS + " days");
        }
    }
}
