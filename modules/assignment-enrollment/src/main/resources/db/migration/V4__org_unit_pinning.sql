-- Ch.19: which org unit the enrolled learner belongs to, if any (opt-in).
-- Propagated to Certification via the Published Language message so a
-- manager-scoped revocation check has something to compare against.
alter table enrollment_projection add column org_unit_id uuid;
