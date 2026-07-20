-- Ch.19: which org unit "owns" this course, if any — opt-in, used only for
-- manager-scoped authorization on create/publish, never a hard requirement.
alter table courses add column org_unit_id uuid;
