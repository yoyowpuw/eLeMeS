-- Ch.12 §2: Row-Level Security as defense-in-depth beneath application-layer
-- tenant scoping — see course-management's identically-purposed migration
-- for the full rationale (FORCE requirement, why this isn't redundant with
-- the app-layer OPA check).
--
-- org_closure deliberately has NO tenant_id column and NO policy of its
-- own — it stores pure topology (unit-id pairs), no tenant-identifying
-- data. It's protected transitively: every read joins it to org_units,
-- and that join's RLS-filtered result set is what actually reaches the
-- caller.
alter table org_units enable row level security;
alter table org_units force row level security;

create policy tenant_isolation on org_units
    using (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*')
    with check (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*');
