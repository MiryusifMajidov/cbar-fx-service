package az.yusif.fx.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "fx")
public record FxProperties(@Valid @NotNull Cbar cbar,
                           @Valid @NotNull Sync sync,
                           @Valid @NotNull Security security) {

    public record Cbar(@NotBlank String baseUrl,
                       @NotNull Duration connectTimeout,
                       @NotNull Duration readTimeout) {
    }

    public record Sync(boolean enabled,
                       @NotBlank String cron,
                       @NotBlank String zone,
                       @Min(1) int maxBackfillDays) {
    }

    public record Security(@NotBlank String adminKey,
                           @Min(1) int defaultRequestsPerMinute) {
    }
}
