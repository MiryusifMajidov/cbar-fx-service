package az.yusif.fx.client;

import az.yusif.fx.client.xml.ValCurs;
import az.yusif.fx.exception.RatesNotPublishedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class CbarClient {

    private static final Logger log = LoggerFactory.getLogger(CbarClient.class);
    private static final DateTimeFormatter PATH_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final RestClient restClient;

    public CbarClient(@Qualifier("cbarRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retryable(retryFor = RestClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public ValCurs fetchRates(LocalDate date) {
        log.debug("Fetching CBAR rates for {}", date);

        ValCurs valCurs = restClient.get()
                .uri("/currencies/{date}.xml", date.format(PATH_DATE))
                .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                    throw new RatesNotPublishedException(date);
                })
                .body(ValCurs.class);

        if (valCurs == null || valCurs.types() == null || valCurs.types().isEmpty()) {
            throw new RatesNotPublishedException(date);
        }
        return valCurs;
    }
}
