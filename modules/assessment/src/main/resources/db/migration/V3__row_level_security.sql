-- Ch.12 §2: Row-Level Security as defense-in-depth beneath application-layer
-- tenant scoping — see course-management's identically-purposed migration
-- for the full rationale (FORCE requirement, why this isn't redundant with
-- the app-layer OPA check).
alter table assessment_events enable row level security;
alter table assessment_events force row level security;

create policy tenant_isolation on assessment_events
    using (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*')
    with check (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*');

alter table assessment_projection enable row level security;
alter table assessment_projection force row level security;

create policy tenant_isolation on assessment_projection
    using (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*')
    with check (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*');
