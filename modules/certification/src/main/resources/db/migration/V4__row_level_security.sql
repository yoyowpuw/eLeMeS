-- Ch.12 §2: Row-Level Security as defense-in-depth beneath application-layer
-- tenant scoping — see course-management's identically-purposed migration
-- for the full rationale (FORCE requirement, why this isn't redundant with
-- the app-layer OPA check). certificate_projection is also what `/verify`
-- reads with no tenant at all (Ch.26 §6) — that's TenantContext.BYPASS's
-- one legitimate call site, not a gap in this policy.
alter table certificate_events enable row level security;
alter table certificate_events force row level security;

create policy tenant_isolation on certificate_events
    using (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*')
    with check (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*');

alter table certificate_projection enable row level security;
alter table certificate_projection force row level security;

create policy tenant_isolation on certificate_projection
    using (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*')
    with check (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*');
