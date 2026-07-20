create table outbox (
    id           uuid primary key,
    topic        varchar(128) not null,
    message_key  varchar(128) not null,
    payload      text not null,
    created_at   timestamptz not null,
    published_at timestamptz
);

create index idx_outbox_unpublished on outbox (created_at) where published_at is null;
