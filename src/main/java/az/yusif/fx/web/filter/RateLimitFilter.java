package az.yusif.fx.web.filter;

import az.yusif.fx.domain.ApiClient;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class RateLimitFilter extends OncePerRequestFilter {

    static final String REMAINING_HEADER = "X-Rate-Limit-Remaining";

    private final Cache<Long, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(10_000)
            .build();

    private final ErrorResponseWriter errorWriter;

    public RateLimitFilter(ErrorResponseWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(request.getAttribute(ApiKeyFilter.CLIENT_ATTRIBUTE) instanceof ApiClient);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        ApiClient client = (ApiClient) request.getAttribute(ApiKeyFilter.CLIENT_ATTRIBUTE);
        Bucket bucket = buckets.get(client.id(), id -> newBucket(client.requestsPerMinute()));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader(REMAINING_HEADER, String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfter = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        errorWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                "Limit of " + client.requestsPerMinute() + " requests per minute exceeded");
    }

    private static Bucket newBucket(int requestsPerMinute) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(requestsPerMinute)
                        .refillGreedy(requestsPerMinute, Duration.ofMinutes(1)))
                .build();
    }
}
