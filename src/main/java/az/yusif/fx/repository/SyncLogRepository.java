package az.yusif.fx.repository;

import az.yusif.fx.domain.SyncLog;
import az.yusif.fx.domain.SyncStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class SyncLogRepository {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private final JdbcClient jdbcClient;

    public SyncLogRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public long start(LocalDate requestedDate) {
        return jdbcClient.sql("""
                        insert into sync_log (requested_date, status)
                        values (:date, :status)
                        returning id
                        """)
                .param("date", requestedDate)
                .param("status", SyncStatus.RUNNING.name())
                .query(Long.class)
                .single();
    }

    public void finish(long id, SyncStatus status, int ratesCount, String message) {
        jdbcClient.sql("""
                        update sync_log
                        set status      = :status,
                            rates_count = :ratesCount,
                            message     = :message,
                            finished_at = now()
                        where id = :id
                        """)
                .param("id", id)
                .param("status", status.name())
                .param("ratesCount", ratesCount)
                .param("message", truncate(message))
                .update();
    }

    public List<SyncLog> findRecent(int limit) {
        return jdbcClient.sql("""
                        select id, requested_date, status, rates_count, message, started_at, finished_at
                        from sync_log
                        order by started_at desc
                        limit :limit
                        """)
                .param("limit", limit)
                .query((rs, rowNum) -> new SyncLog(
                        rs.getLong("id"),
                        rs.getObject("requested_date", LocalDate.class),
                        SyncStatus.valueOf(rs.getString("status")),
                        rs.getObject("rates_count", Integer.class),
                        rs.getString("message"),
                        rs.getObject("started_at", OffsetDateTime.class),
                        rs.getObject("finished_at", OffsetDateTime.class)))
                .list();
    }

    private static String truncate(String message) {
        if (message == null || message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH);
    }
}
