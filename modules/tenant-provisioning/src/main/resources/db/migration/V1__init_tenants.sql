-- Ch.18 §2: the control plane's tenant registry. Deliberately NOT
-- Row-Level Security-protected the way every other service's tables are
-- (Ch.12 §2) — a row here doesn't belong to the tenant it describes the
-- way a course or enrollment belongs to a tenant; it's platform-level
-- registry data. Real access control for who may list/manage other
-- tenants' registry entries is a known gap, documented in README.
create table tenants (
    tenant_id      varchar(64) primary key,
    name           varchar(256) not null,
    isolation_tier varchar(16) not null,
    region         varchar(32) not null,
    status         varchar(16) not null,
    created_at     timestamptz not null,
    updated_at     timestamptz not null
);
