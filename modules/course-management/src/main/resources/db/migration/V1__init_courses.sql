create table courses (
    course_id  uuid primary key,
    tenant_id  varchar(64) not null,
    code       varchar(64) not null,
    title      varchar(256) not null,
    created_at timestamptz not null
);

create index idx_courses_tenant on courses (tenant_id);
