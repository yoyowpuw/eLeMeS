-- Ch.12 §2: Row-Level Security as defense-in-depth beneath application-layer
-- tenant scoping for pooled clusters. CourseRepository.findById() (and
-- others) don't filter by tenant_id in their WHERE clause at all — they
-- rely entirely on OPA's app-layer check running afterward on whatever the
-- query returns. RLS is what makes that provably safe even if that check
-- is ever skipped or a future endpoint forgets it, not just "one more
-- layer on top of a check that was already there."
--
-- FORCE (not just ENABLE) is required, or Postgres exempts the table
-- owner — the same role this application connects as — from RLS entirely,
-- silently defeating the whole point.
alter table courses enable row level security;
alter table courses force row level security;

create policy tenant_isolation on courses
    using (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*')
    with check (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*');

alter table content_versions enable row level security;
alter table content_versions force row level security;

create policy tenant_isolation on content_versions
    using (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*')
    with check (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*');
