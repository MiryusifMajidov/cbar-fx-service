package az.yusif.fx.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(@NotBlank @Size(max = 100) String name,
                                  @Min(1) @Max(10_000) Integer requestsPerMinute) {
}
