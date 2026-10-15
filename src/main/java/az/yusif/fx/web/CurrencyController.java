package az.yusif.fx.web;

import az.yusif.fx.domain.Currency;
import az.yusif.fx.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/currencies")
@Tag(name = "Currencies")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping
    @Operation(summary = "Currencies and bank metals quoted by CBAR")
    public List<Currency> getCurrencies() {
        return currencyService.findAll();
    }
}
