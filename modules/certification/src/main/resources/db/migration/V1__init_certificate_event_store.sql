create table certificate_events (
    event_id        uuid primary key,
    tenant_id       varchar(64) not null,
    aggregate_id    uuid not null,
    aggregate_type  varchar(64) not null,
    sequence_number bigint not null,
    event_type      varchar(128) not null,
    payload         jsonb not null,
    occurred_at     timestamptz not null,
    constraint uq_certificate_events_aggregate_sequence unique (aggregate_id, sequence_number)
);

create index idx_certificate_events_tenant_aggregate
    on certificate_events (tenant_id, aggregate_id);

-- Ch.5 ADR-005 / Ch.26 §2: one certificate per enrollment. This unique
-- constraint is also the idempotency guard against duplicate Kafka delivery
-- of the same ContentCompleted/GradingPassed event.
create table certificate_projection (
    certificate_id uuid primary key,
    tenant_id      varchar(64) not null,
    enrollment_id  uuid not null unique,
    learner_id     varchar(128) not null,
    course_id      varchar(128) not null,
    score          int,
    signature      text not null,
    status         varchar(32) not null,
    issued_at      timestamptz not null,
    updated_at     timestamptz not null
);

create index idx_certificate_projection_tenant on certificate_projection (tenant_id);
