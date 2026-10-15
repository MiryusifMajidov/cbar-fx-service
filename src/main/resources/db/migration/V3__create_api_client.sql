create table api_client
(
    id                  bigserial    primary key,
    name                varchar(100) not null,
    key_hash            char(64)     not null unique,
    key_prefix          varchar(16)  not null,
    requests_per_minute integer      not null,
    active              boolean      not null default true,
    created_at          timestamptz  not null default now()
);
