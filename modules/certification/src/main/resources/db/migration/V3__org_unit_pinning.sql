-- Ch.19: the learner's org unit at enrollment time, if any — used only for
-- manager-scoped revocation authorization, never part of the signed payload.
alter table certificate_projection add column org_unit_id uuid;
