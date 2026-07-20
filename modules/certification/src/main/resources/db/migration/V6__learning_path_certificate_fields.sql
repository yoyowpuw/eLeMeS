-- Ch.21 §7 / Ch.26 Blue Team addendum: present only on a certificate that
-- closes out a Learning Path's final step — null for a direct-course
-- certificate, exactly like org_unit_id already is.
alter table certificate_projection add column path_id uuid;
alter table certificate_projection add column path_version_id uuid;
alter table certificate_projection add column realized_step_course_ids text;
