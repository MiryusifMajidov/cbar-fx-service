package az.yusif.fx.repository;

import az.yusif.fx.domain.Currency;
import az.yusif.fx.domain.CurrencyType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class CurrencyRepository {

    private static final String UPSERT = """
            insert into currency (code, name, nominal, type)
            values (:code, :name, :nominal, :type)
            on conflict (code) do update
                set name       = excluded.name,
                    nominal    = excluded.nominal,
                    type       = excluded.type,
                    updated_at = now()
            """;

    private static final RowMapper<Currency> ROW_MAPPER = (rs, rowNum) -> new Currency(
            rs.getString("code"),
            rs.getString("name"),
            rs.getInt("nominal"),
            CurrencyType.valueOf(rs.getString("type")));

    private final JdbcClient jdbcClient;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CurrencyRepository(JdbcClient jdbcClient, NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Currency> findAll() {
        return jdbcClient.sql("select code, name, nominal, type from currency order by type, code")
                .query(ROW_MAPPER)
                .list();
    }

    public Optional<Currency> findByCode(String code) {
        return jdbcClient.sql("select code, name, nominal, type from currency where code = :code")
                .param("code", code)
                .query(ROW_MAPPER)
                .optional();
    }

    public void upsertAll(Collection<Currency> currencies) {
        SqlParameterSource[] batch = currencies.stream()
                .map(currency -> new MapSqlParameterSource()
                        .addValue("code", currency.code())
                        .addValue("name", currency.name())
                        .addValue("nominal", currency.nominal())
                        .addValue("type", currency.type().name()))
                .toArray(SqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(UPSERT, batch);
    }
}
