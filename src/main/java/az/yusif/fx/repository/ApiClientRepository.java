package az.yusif.fx.repository;

import az.yusif.fx.domain.ApiClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ApiClientRepository {

    private static final String COLUMNS = "id, name, key_prefix, requests_per_minute, active, created_at";

    private static final RowMapper<ApiClient> ROW_MAPPER = (rs, rowNum) -> new ApiClient(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("key_prefix"),
            rs.getInt("requests_per_minute"),
            rs.getBoolean("active"),
            rs.getObject("created_at", OffsetDateTime.class));

    private final JdbcClient jdbcClient;

    public ApiClientRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public ApiClient insert(String name, String keyHash, String keyPrefix, int requestsPerMinute) {
        return jdbcClient.sql("""
                        insert into api_client (name, key_hash, key_prefix, requests_per_minute)
                        values (:name, :keyHash, :keyPrefix, :rpm)
                        returning id, name, key_prefix, requests_per_minute, active, created_at
                        """)
                .param("name", name)
                .param("keyHash", keyHash)
                .param("keyPrefix", keyPrefix)
                .param("rpm", requestsPerMinute)
                .query(ROW_MAPPER)
                .single();
    }

    @Cacheable(cacheNames = "api-clients", key = "#keyHash")
    public Optional<ApiClient> findActiveByKeyHash(String keyHash) {
        return jdbcClient.sql("select " + COLUMNS + " from api_client where key_hash = :keyHash and active")
                .param("keyHash", keyHash)
                .query(ROW_MAPPER)
                .optional();
    }

    public List<ApiClient> findAll() {
        return jdbcClient.sql("select " + COLUMNS + " from api_client order by id")
                .query(ROW_MAPPER)
                .list();
    }

    public boolean deactivate(long id) {
        return jdbcClient.sql("update api_client set active = false where id = :id and active")
                .param("id", id)
                .update() > 0;
    }
}
