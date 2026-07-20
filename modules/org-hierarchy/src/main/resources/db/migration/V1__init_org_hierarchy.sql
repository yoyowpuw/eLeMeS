create table org_units (
    org_unit_id     uuid primary key,
    tenant_id       varchar(64) not null,
    name            varchar(256) not null,
    unit_type       varchar(64) not null,
    manager_user_id varchar(128),
    created_at      timestamptz not null
);

create index idx_org_units_tenant on org_units (tenant_id);

-- Ch.19 ADR-031: closure table, one row per (hierarchy_type, ancestor, descendant)
-- pair, including a depth-0 self row for every unit. Storing hierarchy_type
-- in the row (rather than a separate table per type) is what lets the same
-- org_unit participate in several independent hierarchies concurrently
-- (reporting-line vs. cost-center vs. matrixed dotted-line) satisfying
-- FR-009's matrixed-reporting requirement.
create table org_closure (
    hierarchy_type varchar(64) not null,
    ancestor_id    uuid not null references org_units (org_unit_id),
    descendant_id  uuid not null references org_units (org_unit_id),
    depth          int not null,
    primary key (hierarchy_type, ancestor_id, descendant_id)
);

create index idx_org_closure_descendant on org_closure (hierarchy_type, descendant_id);
