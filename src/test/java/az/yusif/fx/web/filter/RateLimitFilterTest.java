package az.yusif.fx.web.filter;

import az.yusif.fx.domain.ApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final RateLimitFilter filter =
            new RateLimitFilter(new ErrorResponseWriter(new ObjectMapper().findAndRegisterModules()));

    @Test
    void blocksClientAfterLimitIsReached() throws Exception {
        var client = new ApiClient(1, "test", "fx_test123", 2, true, OffsetDateTime.now());

        MockHttpServletResponse first = call(client);
        MockHttpServletResponse second = call(client);
        MockHttpServletResponse third = call(client);

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(first.getHeader(RateLimitFilter.REMAINING_HEADER)).isEqualTo("1");
        assertThat(second.getHeader(RateLimitFilter.REMAINING_HEADER)).isEqualTo("0");
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getHeader("Retry-After")).isNotBlank();
    }

    @Test
    void limitsAreTrackedPerClient() throws Exception {
        var busy = new ApiClient(1, "busy", "fx_busy1234", 1, true, OffsetDateTime.now());
        var quiet = new ApiClient(2, "quiet", "fx_quiet123", 1, true, OffsetDateTime.now());

        call(busy);

        assertThat(call(busy).getStatus()).isEqualTo(429);
        assertThat(call(quiet).getStatus()).isEqualTo(200);
    }

    @Test
    void adminRequestsAreNotLimited() throws Exception {
        var response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/admin/clients"), response, new MockFilterChain());

        assertThat(response.getHeader(RateLimitFilter.REMAINING_HEADER)).isNull();
    }

    private MockHttpServletResponse call(ApiClient client) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/rates");
        request.setAttribute(ApiKeyFilter.CLIENT_ATTRIBUTE, client);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
