package az.yusif.fx.web.filter;

import az.yusif.fx.domain.ApiClient;
import az.yusif.fx.service.ApiClientService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String CLIENT_ATTRIBUTE = ApiKeyFilter.class.getName() + ".client";

    private static final String API_PATH = "/api/";
    private static final String ADMIN_PATH = "/api/v1/admin/";

    private final ApiClientService apiClientService;
    private final byte[] adminKey;
    private final ErrorResponseWriter errorWriter;

    public ApiKeyFilter(ApiClientService apiClientService, String adminKey, ErrorResponseWriter errorWriter) {
        this.apiClientService = apiClientService;
        this.adminKey = adminKey.getBytes(StandardCharsets.UTF_8);
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(API_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (!StringUtils.hasText(apiKey)) {
            errorWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Missing " + API_KEY_HEADER + " header");
            return;
        }

        if (isAdminKey(apiKey)) {
            chain.doFilter(request, response);
            return;
        }

        if (request.getRequestURI().startsWith(ADMIN_PATH)) {
            errorWriter.write(request, response, HttpStatus.FORBIDDEN, "Admin key required");
            return;
        }

        Optional<ApiClient> client = apiClientService.authenticate(apiKey);
        if (client.isEmpty()) {
            errorWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Invalid API key");
            return;
        }

        request.setAttribute(CLIENT_ATTRIBUTE, client.get());
        chain.doFilter(request, response);
    }

    private boolean isAdminKey(String apiKey) {
        return MessageDigest.isEqual(adminKey, apiKey.getBytes(StandardCharsets.UTF_8));
    }
}
