-- Ch.12 §2 silo tier: the base JDBC URL (no schema — every data-plane
-- service appends its own) of the dedicated database SiloProvisioner
-- created for this tenant. Null for a POOLED tenant, always.
alter table tenants add column silo_database varchar(512);
