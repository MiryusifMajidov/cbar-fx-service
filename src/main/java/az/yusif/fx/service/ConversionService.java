package az.yusif.fx.service;

import az.yusif.fx.domain.Currency;
import az.yusif.fx.dto.ConversionResponse;
import az.yusif.fx.exception.NotFoundException;
import az.yusif.fx.repository.RateRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class ConversionService {

    private static final int RESULT_SCALE = 4;
    private static final int RATE_SCALE = 6;

    private final RateRepository rateRepository;
    private final Clock clock;

    public ConversionService(RateRepository rateRepository, Clock clock) {
        this.rateRepository = rateRepository;
        this.clock = clock;
    }

    public ConversionResponse convert(String from, String to, BigDecimal amount, @Nullable LocalDate date) {
        String source = from.toUpperCase(Locale.ROOT);
        String target = to.toUpperCase(Locale.ROOT);
        LocalDate onDate = date != null ? date : LocalDate.now(clock);

        Quote sourceQuote = quote(source, onDate);
        Quote targetQuote = quote(target, onDate);

        BigDecimal rate = sourceQuote.unitRate().divide(targetQuote.unitRate(), RATE_SCALE, RoundingMode.HALF_UP);
        BigDecimal result = amount.multiply(sourceQuote.unitRate())
                .divide(targetQuote.unitRate(), RESULT_SCALE, RoundingMode.HALF_UP);

        LocalDate rateDate = Stream.of(sourceQuote.date(), targetQuote.date())
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(onDate);

        return new ConversionResponse(source, target, amount, result, rate, rateDate);
    }

    private Quote quote(String code, LocalDate date) {
        if (Currency.AZN.equals(code)) {
            return new Quote(BigDecimal.ONE, null);
        }
        return rateRepository.findOnOrBefore(code, date)
                .map(rate -> new Quote(rate.unitRate(), rate.rateDate()))
                .orElseThrow(() -> new NotFoundException("No %s rate on or before %s".formatted(code, date)));
    }

    private record Quote(BigDecimal unitRate, LocalDate date) {
    }
}
