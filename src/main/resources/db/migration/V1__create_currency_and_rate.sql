create table currency
(
    code       varchar(3)   primary key,
    name       varchar(128) not null,
    nominal    integer      not null,
    type       varchar(16)  not null,
    updated_at timestamptz  not null default now()
);

create table exchange_rate
(
    id            bigserial     primary key,
    currency_code varchar(3)    not null references currency (code),
    rate_date     date          not null,
    nominal       integer       not null,
    rate          numeric(18, 4) not null,
    created_at    timestamptz   not null default now(),
    constraint uq_exchange_rate_code_date unique (currency_code, rate_date)
);

create index idx_exchange_rate_date on exchange_rate (rate_date);
