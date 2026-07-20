-- Ch.21: LearningPath/PathVersion/PathStep, inlined into course-management
-- (see LearningPathController's doc comment for why no standalone service).
-- Same insert-only versioning shape as courses/content_versions (Ch.12 §7).
create table learning_paths (
    path_id            uuid primary key,
    tenant_id          varchar(64) not null,
    name               varchar(256) not null,
    created_at         timestamptz not null,
    current_version_id uuid,
    org_unit_id        uuid
);

create index idx_learning_paths_tenant on learning_paths (tenant_id);

create table path_versions (
    version_id     uuid primary key,
    tenant_id      varchar(64) not null,
    path_id        uuid not null references learning_paths (path_id),
    version_number int not null,
    created_at     timestamptz not null
);

create index idx_path_versions_path on path_versions (path_id);

-- v1 scope: strict-sequence ordering only (step_order, always consumed in
-- order) — unordered-set/conditional-branch modes are deferred, not modeled.
-- Carries its own tenant_id (denormalized from path_versions) so it gets the
-- same direct RLS policy as every other table here, rather than relying on
-- a join back to path_versions for isolation.
create table path_steps (
    step_id          uuid primary key,
    tenant_id        varchar(64) not null,
    path_version_id  uuid not null references path_versions (version_id),
    step_order       int not null,
    course_id        uuid not null references courses (course_id),
    unique (path_version_id, step_order)
);

create index idx_path_steps_version on path_steps (path_version_id);

alter table learning_paths enable row level security;
alter table learning_paths force row level security;

create policy tenant_isolation on learning_paths
    using (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*')
    with check (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*');

alter table path_versions enable row level security;
alter table path_versions force row level security;

create policy tenant_isolation on path_versions
    using (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*')
    with check (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*');

alter table path_steps enable row level security;
alter table path_steps force row level security;

create policy tenant_isolation on path_steps
    using (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*')
    with check (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*');
