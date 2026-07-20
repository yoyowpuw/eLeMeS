-- Ch.12 §7: content is hash-addressed and insert-only. A "new version" is
-- always a new row — content_versions is never updated in place — so a
-- version referenced by an already-issued certificate remains fetchable
-- forever, even after the course moves on to a newer version.
create table content_versions (
    version_id     uuid primary key,
    tenant_id      varchar(64) not null,
    course_id      uuid not null references courses (course_id),
    version_number int not null,
    content_hash   varchar(128) not null,
    created_at     timestamptz not null,
    constraint uq_content_versions_course_number unique (course_id, version_number)
);

-- Ch.15 §8: tenant_id-leading composite index on the hot query path.
create index idx_content_versions_tenant_course on content_versions (tenant_id, course_id);

-- The course's "current version" is a pointer, not the version data itself —
-- moving this pointer never touches historical content_versions rows.
alter table courses add column current_version_id uuid references content_versions (version_id);
