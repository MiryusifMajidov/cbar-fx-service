package az.yusif.fx.repository;

import az.yusif.fx.domain.CurrencyType;
import az.yusif.fx.domain.ExchangeRate;
import az.yusif.fx.dto.MonthlyRateDto;
import az.yusif.fx.dto.RateChangeDto;
import az.yusif.fx.dto.RateDto;
import az.yusif.fx.dto.RatePoint;
import az.yusif.fx.dto.RateStats;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class RateRepository {

    private static final String UPSERT = """
            insert into exchange_rate (currency_code, rate_date, nominal, rate)
            values (:code, :rateDate, :nominal, :rate)
            on conflict (currency_code, rate_date) do update
                set nominal = excluded.nominal,
                    rate    = excluded.rate
            """;

    private static final String STATS = """
            with period as (
                select rate,
                       first_value(rate) over (order by rate_date)      as first_rate,
                       first_value(rate) over (order by rate_date desc) as last_rate
                from exchange_rate
                where currency_code = :code
                  and rate_date between :from and :to
            )
            select count(*)            as days,
                   min(rate)           as min_rate,
                   max(rate)           as max_rate,
                   round(avg(rate), 4) as avg_rate,
                   max(first_rate)     as first_rate,
                   max(last_rate)      as last_rate,
                   round((max(last_rate) - max(first_rate)) * 100 / nullif(max(first_rate), 0), 4) as change_percent
            from period
            """;

    private static final String DAILY_CHANGES = """
            with recent as (
                select currency_code,
                       rate_date,
                       rate,
                       lag(rate) over (partition by currency_code order by rate_date) as previous_rate
                from exchange_rate
                where rate_date between cast(:date as date) - 14 and :date
            )
            select c.code,
                   c.name,
                   c.nominal,
                   r.rate,
                   r.previous_rate,
                   r.rate - r.previous_rate as change,
                   round((r.rate - r.previous_rate) * 100 / nullif(r.previous_rate, 0), 4) as change_percent
            from recent r
                     join currency c on c.code = r.currency_code
            where r.rate_date = :date
            order by c.type, c.code
            """;

    private static final String MONTHLY = """
            select cast(date_trunc('month', rate_date) as date) as month_start,
                   round(avg(rate), 4)                          as avg_rate,
                   min(rate)                                    as min_rate,
                   max(rate)                                    as max_rate,
                   count(*)                                     as days
            from exchange_rate
            where currency_code = :code
              and rate_date >= make_date(:year, 1, 1)
              and rate_date < make_date(:year + 1, 1, 1)
            group by month_start
            order by month_start
            """;

    private final JdbcClient jdbcClient;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RateRepository(JdbcClient jdbcClient, NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertAll(Collection<ExchangeRate> rates) {
        SqlParameterSource[] batch = rates.stream()
                .map(rate -> new MapSqlParameterSource()
                        .addValue("code", rate.currencyCode())
                        .addValue("rateDate", rate.rateDate())
                        .addValue("nominal", rate.nominal())
                        .addValue("rate", rate.rate()))
                .toArray(SqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(UPSERT, batch);
    }

    public Optional<LocalDate> findLatestDate(LocalDate onOrBefore) {
        return jdbcClient.sql("select max(rate_date) from exchange_rate where rate_date <= :date")
                .param("date", onOrBefore)
                .query(LocalDate.class)
                .optional();
    }

    public List<RateDto> findByDate(LocalDate date) {
        return jdbcClient.sql("""
                        select c.code, c.name, r.nominal, c.type, r.rate
                        from exchange_rate r
                                 join currency c on c.code = r.currency_code
                        where r.rate_date = :date
                        order by c.type, c.code
                        """)
                .param("date", date)
                .query((rs, rowNum) -> new RateDto(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("nominal"),
                        CurrencyType.valueOf(rs.getString("type")),
                        rs.getBigDecimal("rate")))
                .list();
    }

    public Optional<ExchangeRate> findOnOrBefore(String code, LocalDate date) {
        return jdbcClient.sql("""
                        select currency_code, rate_date, nominal, rate
                        from exchange_rate
                        where currency_code = :code
                          and rate_date <= :date
                        order by rate_date desc
                        limit 1
                        """)
                .param("code", code)
                .param("date", date)
                .query((rs, rowNum) -> new ExchangeRate(
                        rs.getString("currency_code"),
                        rs.getObject("rate_date", LocalDate.class),
                        rs.getInt("nominal"),
                        rs.getBigDecimal("rate")))
                .optional();
    }

    public List<RatePoint> findHistory(String code, LocalDate from, LocalDate to) {
        return jdbcClient.sql("""
                        select rate_date, nominal, rate
                        from exchange_rate
                        where currency_code = :code
                          and rate_date between :from and :to
                        order by rate_date
                        """)
                .param("code", code)
                .param("from", from)
                .param("to", to)
                .query((rs, rowNum) -> new RatePoint(
                        rs.getObject("rate_date", LocalDate.class),
                        rs.getInt("nominal"),
                        rs.getBigDecimal("rate")))
                .list();
    }

    public Optional<RateStats> findStats(String code, LocalDate from, LocalDate to) {
        RateStats stats = jdbcClient.sql(STATS)
                .param("code", code)
                .param("from", from)
                .param("to", to)
                .query((rs, rowNum) -> new RateStats(
                        code,
                        from,
                        to,
                        rs.getInt("days"),
                        rs.getBigDecimal("min_rate"),
                        rs.getBigDecimal("max_rate"),
                        rs.getBigDecimal("avg_rate"),
                        rs.getBigDecimal("first_rate"),
                        rs.getBigDecimal("last_rate"),
                        rs.getBigDecimal("change_percent")))
                .single();
        return stats.days() == 0 ? Optional.empty() : Optional.of(stats);
    }

    public List<RateChangeDto> findChanges(LocalDate date) {
        return jdbcClient.sql(DAILY_CHANGES)
                .param("date", date)
                .query((rs, rowNum) -> new RateChangeDto(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("nominal"),
                        rs.getBigDecimal("rate"),
                        rs.getBigDecimal("previous_rate"),
                        rs.getBigDecimal("change"),
                        rs.getBigDecimal("change_percent")))
                .list();
    }

    public List<MonthlyRateDto> findMonthly(String code, int year) {
        return jdbcClient.sql(MONTHLY)
                .param("code", code)
                .param("year", year)
                .query((rs, rowNum) -> new MonthlyRateDto(
                        YearMonth.from(rs.getObject("month_start", LocalDate.class)),
                        rs.getBigDecimal("avg_rate"),
                        rs.getBigDecimal("min_rate"),
                        rs.getBigDecimal("max_rate"),
                        rs.getInt("days")))
                .list();
    }
}
