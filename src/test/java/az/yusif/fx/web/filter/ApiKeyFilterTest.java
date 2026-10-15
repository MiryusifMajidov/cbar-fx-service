package az.yusif.fx.web.filter;

import az.yusif.fx.domain.ApiClient;
import az.yusif.fx.service.ApiClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApiKeyFilterTest {

    private final ApiClientService apiClientService = mock(ApiClientService.class);
    private final ApiKeyFilter filter = new ApiKeyFilter(apiClientService, "admin-secret",
            new ErrorResponseWriter(new ObjectMapper().findAndRegisterModules()));

    @Test
    void rejectsRequestWithoutKey() throws Exception {
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/rates"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Missing X-API-Key header");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void skipsNonApiPaths() throws Exception {
        var chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("GET", "/swagger-ui.html"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void adminKeyOpensAdminEndpoints() throws Exception {
        var request = requestWithKey("/api/v1/admin/sync", "admin-secret");
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(apiClientService);
    }

    @Test
    void clientKeyCannotReachAdminEndpoints() throws Exception {
        var response = new MockHttpServletResponse();

        filter.doFilter(requestWithKey("/api/v1/admin/clients", "fx_client"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void unknownKeyIsRejected() throws Exception {
        when(apiClientService.authenticate(anyString())).thenReturn(Optional.empty());
        var response = new MockHttpServletResponse();

        filter.doFilter(requestWithKey("/api/v1/rates", "fx_unknown"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid API key");
    }

    @Test
    void authenticatedClientIsExposedToNextFilters() throws Exception {
        var client = new ApiClient(3, "mobile-app", "fx_abcdefg", 60, true, OffsetDateTime.now());
        when(apiClientService.authenticate("fx_valid")).thenReturn(Optional.of(client));
        var request = requestWithKey("/api/v1/rates", "fx_valid");
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(request.getAttribute(ApiKeyFilter.CLIENT_ATTRIBUTE)).isEqualTo(client);
        verify(apiClientService).authenticate("fx_valid");
    }

    private static MockHttpServletRequest requestWithKey(String uri, String key) {
        var request = new MockHttpServletRequest("GET", uri);
        request.addHeader(ApiKeyFilter.API_KEY_HEADER, key);
        return request;
    }
}
