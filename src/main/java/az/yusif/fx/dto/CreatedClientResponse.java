package az.yusif.fx.dto;

public record CreatedClientResponse(long id, String name, String apiKey, int requestsPerMinute) {
}
