-- Ch.5 ADR-005 / Ch.21 §7: the certificate must record which exact content
-- version was pinned at enrollment time, not just a course identifier.
alter table certificate_projection add column content_version_id uuid not null default '00000000-0000-0000-0000-000000000000';
alter table certificate_projection alter column content_version_id drop default;
