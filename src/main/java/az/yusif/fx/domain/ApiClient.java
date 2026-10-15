package az.yusif.fx.domain;

import java.time.OffsetDateTime;

public record ApiClient(long id,
                        String name,
                        String keyPrefix,
                        int requestsPerMinute,
                        boolean active,
                        OffsetDateTime createdAt) {
}
