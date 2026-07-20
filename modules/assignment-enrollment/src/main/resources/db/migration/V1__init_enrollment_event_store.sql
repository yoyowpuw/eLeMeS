-- Ch.12 §5: append-only event log is the system of record for the
-- compliance-critical tier. Ch.15 §8: every pooled-cluster table requires a
-- tenant_id-leading composite index on hot query paths (NFR-001).

create table enrollment_events (
    event_id        uuid primary key,
    tenant_id       varchar(64) not null,
    aggregate_id    uuid not null,
    aggregate_type  varchar(64) not null,
    sequence_number bigint not null,
    event_type      varchar(128) not null,
    payload         jsonb not null,
    occurred_at     timestamptz not null,
    constraint uq_enrollment_events_aggregate_sequence unique (aggregate_id, sequence_number)
);

create index idx_enrollment_events_tenant_aggregate
    on enrollment_events (tenant_id, aggregate_id);

-- Read-optimized projection (Ch.12 §5) kept in sync in the same transaction
-- as each event append.
create table enrollment_projection (
    enrollment_id    uuid primary key,
    tenant_id        varchar(64) not null,
    learner_id       varchar(128) not null,
    course_id        varchar(128) not null,
    status           varchar(32) not null,
    progress_percent int not null default 0,
    updated_at       timestamptz not null
);

create index idx_enrollment_projection_tenant
    on enrollment_projection (tenant_id);
