-- Ch.12 §2 silo tier: the same non-superuser role as the pooled cluster
-- (see init-app-role.sql), on the *separate* Postgres instance standing in
-- for "dedicated cluster infrastructure." No upfront database grant here —
-- unlike the pooled cluster's single shared `elemes` database, a silo
-- tenant's database doesn't exist yet at cluster-init time; it's created
-- dynamically per tenant (see tenant-provisioning's SiloProvisioner), which
-- grants CREATE+CONNECT on that specific database to this role at that point.
create role elemes_app with login password 'elemes_app_local_dev';
