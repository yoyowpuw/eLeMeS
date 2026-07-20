create table assessment_events (
    event_id        uuid primary key,
    tenant_id       varchar(64) not null,
    aggregate_id    uuid not null,
    aggregate_type  varchar(64) not null,
    sequence_number bigint not null,
    event_type      varchar(128) not null,
    payload         jsonb not null,
    occurred_at     timestamptz not null,
    constraint uq_assessment_events_aggregate_sequence unique (aggregate_id, sequence_number)
);

create index idx_assessment_events_tenant_aggregate
    on assessment_events (tenant_id, aggregate_id);

create table assessment_projection (
    assessment_id  uuid primary key,
    tenant_id      varchar(64) not null,
    enrollment_id  uuid not null,
    status         varchar(32) not null,
    score          int,
    updated_at     timestamptz not null
);

create index idx_assessment_projection_tenant on assessment_projection (tenant_id);
create index idx_assessment_projection_enrollment on assessment_projection (enrollment_id);
