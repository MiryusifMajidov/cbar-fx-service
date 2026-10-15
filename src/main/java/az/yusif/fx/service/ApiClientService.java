package az.yusif.fx.service;

import az.yusif.fx.config.FxProperties;
import az.yusif.fx.domain.ApiClient;
import az.yusif.fx.dto.CreateClientRequest;
import az.yusif.fx.dto.CreatedClientResponse;
import az.yusif.fx.exception.NotFoundException;
import az.yusif.fx.repository.ApiClientRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class ApiClientService {

    private static final String KEY_PREFIX = "fx_";
    private static final int KEY_BYTES = 24;
    private static final int VISIBLE_PREFIX_LENGTH = 10;

    private final ApiClientRepository repository;
    private final int defaultRequestsPerMinute;
    private final SecureRandom random = new SecureRandom();

    public ApiClientService(ApiClientRepository repository, FxProperties properties) {
        this.repository = repository;
        this.defaultRequestsPerMinute = properties.security().defaultRequestsPerMinute();
    }

    public CreatedClientResponse create(CreateClientRequest request) {
        String apiKey = generateKey();
        int requestsPerMinute = request.requestsPerMinute() != null
                ? request.requestsPerMinute()
                : defaultRequestsPerMinute;

        ApiClient client = repository.insert(
                request.name().trim(),
                hash(apiKey),
                apiKey.substring(0, VISIBLE_PREFIX_LENGTH),
                requestsPerMinute);

        return new CreatedClientResponse(client.id(), client.name(), apiKey, client.requestsPerMinute());
    }

    public Optional<ApiClient> authenticate(String apiKey) {
        if (!apiKey.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }
        return repository.findActiveByKeyHash(hash(apiKey));
    }

    public List<ApiClient> findAll() {
        return repository.findAll();
    }

    @CacheEvict(cacheNames = "api-clients", allEntries = true)
    public void deactivate(long id) {
        if (!repository.deactivate(id)) {
            throw new NotFoundException("Active client " + id + " not found");
        }
    }

    private String generateKey() {
        byte[] bytes = new byte[KEY_BYTES];
        random.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
