package az.yusif.fx.web;

import az.yusif.fx.domain.ExchangeRate;
import az.yusif.fx.dto.DailyChangesResponse;
import az.yusif.fx.dto.DailyRatesResponse;
import az.yusif.fx.dto.MonthlyRateDto;
import az.yusif.fx.dto.RateHistoryResponse;
import az.yusif.fx.dto.RateStats;
import az.yusif.fx.service.RateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rates")
@Tag(name = "Rates")
public class RateController {

    private final RateService rateService;

    public RateController(RateService rateService) {
        this.rateService = rateService;
    }

    @GetMapping
    @Operation(summary = "All rates for a date",
            description = "Falls back to the latest published date when the requested date has no rates")
    public DailyRatesResponse getRates(@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        return rateService.getDailyRates(date);
    }

    @GetMapping("/changes")
    @Operation(summary = "Day-over-day change for every currency")
    public DailyChangesResponse getChanges(@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        return rateService.getChanges(date);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Rate history for a currency", description = "Defaults to the last 30 days")
    public RateHistoryResponse getHistory(@PathVariable @CurrencyCode String code,
                                          @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return rateService.getHistory(code, from, to);
    }

    @GetMapping("/{code}/latest")
    @Operation(summary = "Latest stored rate for a currency")
    public ExchangeRate getLatest(@PathVariable @CurrencyCode String code) {
        return rateService.getLatest(code);
    }

    @GetMapping("/{code}/stats")
    @Operation(summary = "Min, max, average and change over a period")
    public RateStats getStats(@PathVariable @CurrencyCode String code,
                              @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
                              @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return rateService.getStats(code, from, to);
    }

    @GetMapping("/{code}/monthly")
    @Operation(summary = "Monthly averages for a year")
    public List<MonthlyRateDto> getMonthly(@PathVariable @CurrencyCode String code,
                                           @RequestParam(required = false) @Min(1992) @Max(2100) Integer year) {
        return rateService.getMonthly(code, year);
    }
}
