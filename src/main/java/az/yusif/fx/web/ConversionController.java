package az.yusif.fx.web;

import az.yusif.fx.dto.ConversionResponse;
import az.yusif.fx.service.ConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@Tag(name = "Conversion")
public class ConversionController {

    private final ConversionService conversionService;

    public ConversionController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping("/api/v1/convert")
    @Operation(summary = "Convert an amount between two currencies using CBAR cross rates")
    public ConversionResponse convert(@RequestParam @CurrencyCode String from,
                                      @RequestParam @CurrencyCode String to,
                                      @RequestParam @Positive @DecimalMax("1000000000000") BigDecimal amount,
                                      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        return conversionService.convert(from, to, amount, date);
    }
}
