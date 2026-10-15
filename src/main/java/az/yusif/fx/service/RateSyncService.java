package az.yusif.fx.service;

import az.yusif.fx.client.CbarClient;
import az.yusif.fx.client.xml.ValCurs;
import az.yusif.fx.client.xml.ValType;
import az.yusif.fx.client.xml.Valute;
import az.yusif.fx.config.FxProperties;
import az.yusif.fx.domain.Currency;
import az.yusif.fx.domain.CurrencyType;
import az.yusif.fx.domain.ExchangeRate;
import az.yusif.fx.domain.SyncLog;
import az.yusif.fx.domain.SyncStatus;
import az.yusif.fx.dto.BackfillResult;
import az.yusif.fx.dto.SyncResult;
import az.yusif.fx.exception.InvalidRequestException;
import az.yusif.fx.exception.RatesNotPublishedException;
import az.yusif.fx.repository.CurrencyRepository;
import az.yusif.fx.repository.RateRepository;
import az.yusif.fx.repository.SyncLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class RateSyncService {

    private static final Logger log = LoggerFactory.getLogger(RateSyncService.class);
    private static final DateTimeFormatter CBAR_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final CbarClient cbarClient;
    private final CurrencyRepository currencyRepository;
    private final RateRepository rateRepository;
    private final SyncLogRepository syncLogRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final int maxBackfillDays;

    public RateSyncService(CbarClient cbarClient,
                           CurrencyRepository currencyRepository,
                           RateRepository rateRepository,
                           SyncLogRepository syncLogRepository,
                           TransactionTemplate transactionTemplate,
                           Clock clock,
                           FxProperties properties) {
        this.cbarClient = cbarClient;
        this.currencyRepository = currencyRepository;
        this.rateRepository = rateRepository;
        this.syncLogRepository = syncLogRepository;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.maxBackfillDays = properties.sync().maxBackfillDays();
    }

    @CacheEvict(cacheNames = {"currencies", "daily-rates"}, allEntries = true)
    public SyncResult sync(@Nullable LocalDate date) {
        LocalDate requested = date != null ? date : LocalDate.now(clock);
        long logId = syncLogRepository.start(requested);
        try {
            SyncResult result = fetchAndStore(requested);
            syncLogRepository.finish(logId, SyncStatus.SUCCESS, result.ratesCount(), null);
            log.info("Stored {} CBAR rates for {}", result.ratesCount(), result.rateDate());
            return result;
        } catch (RatesNotPublishedException e) {
            syncLogRepository.finish(logId, SyncStatus.NOT_PUBLISHED, 0, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            syncLogRepository.finish(logId, SyncStatus.FAILED, 0, e.getMessage());
            throw e;
        }
    }

    @CacheEvict(cacheNames = {"currencies", "daily-rates"}, allEntries = true)
    public BackfillResult backfill(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }
        if (to.isAfter(LocalDate.now(clock).plusDays(1))) {
            throw new InvalidRequestException("Cannot backfill future dates");
        }
        if (ChronoUnit.DAYS.between(from, to) >= maxBackfillDays) {
            throw new InvalidRequestException("Backfill range must be shorter than " + maxBackfillDays + " days");
        }

        int synced = 0;
        List<LocalDate> notPublished = new ArrayList<>();
        List<LocalDate> failed = new ArrayList<>();

        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            try {
                sync(day);
                synced++;
            } catch (RatesNotPublishedException e) {
                notPublished.add(day);
            } catch (RuntimeException e) {
                log.warn("Backfill failed for {}: {}", day, e.getMessage());
                failed.add(day);
            }
        }
        return new BackfillResult(from, to, synced, notPublished, failed);
    }

    public List<SyncLog> recentLogs(int limit) {
        return syncLogRepository.findRecent(limit);
    }

    private SyncResult fetchAndStore(LocalDate requested) {
        ValCurs valCurs = cbarClient.fetchRates(requested);
        LocalDate rateDate = parseDate(valCurs.date(), requested);

        List<Currency> currencies = new ArrayList<>();
        List<ExchangeRate> rates = new ArrayList<>();

        for (ValType valType : valCurs.types()) {
            if (valType.valutes() == null) {
                continue;
            }
            CurrencyType type = valType.isMetal() ? CurrencyType.METAL : CurrencyType.FOREIGN;
            for (Valute valute : valType.valutes()) {
                if (valute.code() == null || valute.value() == null) {
                    continue;
                }
                int nominal = valute.nominalAsInt();
                currencies.add(new Currency(valute.code(), cleanName(valute.name(), valute.code()), nominal, type));
                rates.add(new ExchangeRate(valute.code(), rateDate, nominal, valute.value()));
            }
        }

        if (rates.isEmpty()) {
            throw new RatesNotPublishedException(requested);
        }

        transactionTemplate.executeWithoutResult(status -> {
            currencyRepository.upsertAll(currencies);
            rateRepository.upsertAll(rates);
        });
        return new SyncResult(requested, rateDate, rates.size());
    }

    private static LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return LocalDate.parse(value.trim(), CBAR_DATE);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private static String cleanName(String name, String code) {
        if (name == null || name.isBlank()) {
            return code;
        }
        return name.trim().replaceFirst("^\\d+\\s+", "");
    }
}
