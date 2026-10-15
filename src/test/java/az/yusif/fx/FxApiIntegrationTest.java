package az.yusif.fx;

import az.yusif.fx.client.CbarClient;
import az.yusif.fx.dto.CreateClientRequest;
import az.yusif.fx.dto.CreatedClientResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"fx.sync.enabled=false", "fx.security.admin-key=it-admin-key"})
@Testcontainers(disabledWithoutDocker = true)
class FxApiIntegrationTest {

    private static final String ADMIN_KEY = "it-admin-key";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate rest;

    @MockitoBean
    private CbarClient cbarClient;

    @Test
    void syncThenQueryWithClientKey() {
        when(cbarClient.fetchRates(any())).thenReturn(CbarFixtures.load(CbarFixtures.SAMPLE));

        ResponseEntity<JsonNode> sync = exchange(HttpMethod.POST, "/api/v1/admin/sync?date=2026-10-15", ADMIN_KEY, null);
        assertThat(sync.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sync.getBody().get("ratesCount").asInt()).isEqualTo(6);

        ResponseEntity<CreatedClientResponse> created = rest.exchange("/api/v1/admin/clients", HttpMethod.POST,
                new HttpEntity<>(new CreateClientRequest("web-app", 100), headers(ADMIN_KEY)),
                CreatedClientResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String clientKey = created.getBody().apiKey();

        ResponseEntity<JsonNode> rates = exchange(HttpMethod.GET, "/api/v1/rates?date=2026-10-15", clientKey, null);
        assertThat(rates.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rates.getBody().get("rates")).hasSize(6);
        assertThat(rates.getHeaders().getFirst("X-Rate-Limit-Remaining")).isEqualTo("99");

        ResponseEntity<JsonNode> converted = exchange(HttpMethod.GET,
                "/api/v1/convert?from=USD&to=AZN&amount=10&date=" + LocalDate.of(2026, 10, 15), clientKey, null);
        assertThat(converted.getBody().get("result").decimalValue()).isEqualByComparingTo("17");
    }

    @Test
    void requestsWithoutKeyAreRejected() {
        ResponseEntity<JsonNode> response = rest.getForEntity("/api/v1/currencies", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void healthEndpointIsPublic() {
        ResponseEntity<String> response = rest.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<JsonNode> exchange(HttpMethod method, String url, String key, Object body) {
        return rest.exchange(url, method, new HttpEntity<>(body, headers(key)), JsonNode.class);
    }

    private static HttpHeaders headers(String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", key);
        return headers;
    }
}
