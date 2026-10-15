create table sync_log
(
    id             bigserial    primary key,
    requested_date date         not null,
    status         varchar(16)  not null,
    rates_count    integer,
    message        varchar(500),
    started_at     timestamptz  not null default now(),
    finished_at    timestamptz
);

create index idx_sync_log_started_at on sync_log (started_at desc);
