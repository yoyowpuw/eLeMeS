-- Ch.21 §2: which Learning Path step (if any) an enrollment belongs to.
-- Null for a plain direct-course enrollment.
alter table enrollment_projection add column path_progress_id uuid;

-- Ch.21 §2: "a read-model projection off Enrollment events, not a duplicate
-- source of truth" — see PathProgress.kt's doc comment. step_plan and
-- realized_step_course_ids are both stored as JSON text (mirroring how this
-- service already keeps things simple over relational-normalizing every
-- list-shaped field); nothing here queries into them with SQL, only the
-- owning service via ObjectMapper.
create table path_progress (
    path_progress_id         uuid primary key,
    tenant_id                varchar(64) not null,
    learner_id                varchar(128) not null,
    path_id                  uuid not null,
    path_version_id          uuid not null,
    step_plan                text not null,
    current_step_index       int not null,
    status                    varchar(32) not null,
    realized_step_course_ids text not null,
    created_at                timestamptz not null,
    updated_at                timestamptz not null
);

create index idx_path_progress_tenant on path_progress (tenant_id);

alter table path_progress enable row level security;
alter table path_progress force row level security;

create policy tenant_isolation on path_progress
    using (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*')
    with check (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*');
