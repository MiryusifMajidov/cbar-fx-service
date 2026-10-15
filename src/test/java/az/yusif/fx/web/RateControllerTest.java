package az.yusif.fx.web;

import az.yusif.fx.domain.CurrencyType;
import az.yusif.fx.domain.ExchangeRate;
import az.yusif.fx.dto.ConversionResponse;
import az.yusif.fx.dto.DailyRatesResponse;
import az.yusif.fx.dto.RateDto;
import az.yusif.fx.exception.InvalidRequestException;
import az.yusif.fx.exception.NotFoundException;
import az.yusif.fx.service.ConversionService;
import az.yusif.fx.service.RateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RateController.class, ConversionController.class})
class RateControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 15);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateService rateService;

    @MockitoBean
    private ConversionService conversionService;

    @Test
    void returnsRatesForDate() throws Exception {
        when(rateService.getDailyRates(DATE)).thenReturn(new DailyRatesResponse(DATE, List.of(
                new RateDto("USD", "ABŞ dolları", 1, CurrencyType.FOREIGN, new BigDecimal("1.7000")))));

        mockMvc.perform(get("/api/v1/rates").param("date", "2026-10-15"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.date").value("2026-10-15"))
                .andExpect(jsonPath("$.rates[0].code").value("USD"))
                .andExpect(jsonPath("$.rates[0].type").value("FOREIGN"))
                .andExpect(jsonPath("$.rates[0].rate").value(1.7));
    }

    @Test
    void respondsWithJsonEvenWhenXmlIsAccepted() throws Exception {
        when(rateService.getLatest("USD"))
                .thenReturn(new ExchangeRate("USD", DATE, 1, new BigDecimal("1.7000")));

        mockMvc.perform(get("/api/v1/rates/USD/latest").header("Accept", "application/xml, */*"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.rateDate").value("2026-10-15"));
    }

    @Test
    void rejectsMalformedCurrencyCode() throws Exception {
        mockMvc.perform(get("/api/v1/rates/US1/latest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.code").exists());

        verifyNoInteractions(rateService);
    }

    @Test
    void mapsNotFoundTo404() throws Exception {
        when(rateService.getLatest("GBP")).thenThrow(new NotFoundException("Unknown currency: GBP"));

        mockMvc.perform(get("/api/v1/rates/GBP/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Unknown currency: GBP"))
                .andExpect(jsonPath("$.path").value("/api/v1/rates/GBP/latest"));
    }

    @Test
    void statsRequireBothDates() throws Exception {
        mockMvc.perform(get("/api/v1/rates/USD/stats").param("from", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'to' is missing"));
    }

    @Test
    void invalidRangeIsBadRequest() throws Exception {
        when(rateService.getStats(eq("USD"), any(), any()))
                .thenThrow(new InvalidRequestException("'from' must not be after 'to'"));

        mockMvc.perform(get("/api/v1/rates/USD/stats")
                        .param("from", "2026-10-15")
                        .param("to", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'from' must not be after 'to'"));
    }

    @Test
    void convertsAmount() throws Exception {
        when(conversionService.convert("USD", "EUR", new BigDecimal("100"), null))
                .thenReturn(new ConversionResponse("USD", "EUR", new BigDecimal("100"),
                        new BigDecimal("85.9237"), new BigDecimal("0.859237"), DATE));

        mockMvc.perform(get("/api/v1/convert")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(85.9237));
    }

    @Test
    void rejectsNegativeAmount() throws Exception {
        mockMvc.perform(get("/api/v1/convert")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("amount", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());

        verifyNoInteractions(conversionService);
    }
}
