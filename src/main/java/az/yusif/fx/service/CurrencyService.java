package az.yusif.fx.service;

import az.yusif.fx.domain.Currency;
import az.yusif.fx.exception.NotFoundException;
import az.yusif.fx.repository.CurrencyRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    public CurrencyService(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @Cacheable("currencies")
    public List<Currency> findAll() {
        return currencyRepository.findAll();
    }

    public Currency getByCode(String code) {
        String normalized = code.toUpperCase(Locale.ROOT);
        return currencyRepository.findByCode(normalized)
                .orElseThrow(() -> new NotFoundException("Unknown currency: " + normalized));
    }
}
