import { useState } from "react";
import { useAuth } from "react-oidc-context";
import { useActivateTenant, useCreateTenant, useMigrateTenantToSilo, useOffboardTenant, useTenant, useTenants } from "../api/tenants";
import { rolesFromAccessToken, decodeAccessToken } from "../auth/jwt";
import { ApiError } from "../api/http";
import type { Tenant } from "../api/types";

export function TenantsPage() {
  const auth = useAuth();
  const roles = rolesFromAccessToken(auth.user?.access_token);
  const isPlatformAdmin = roles.includes("platform-admin");
  const ownTenantId = auth.user?.access_token ? decodeAccessToken(auth.user.access_token).tenant_id : undefined;

  if (isPlatformAdmin) return <PlatformAdminView />;
  return <OwnTenantView tenantId={ownTenantId} />;
}

function PlatformAdminView() {
  const { data: tenants, isLoading } = useTenants(true);
  const createTenant = useCreateTenant();
  const activateTenant = useActivateTenant();
  const offboardTenant = useOffboardTenant();
  const migrateTenant = useMigrateTenantToSilo();

  const [tenantId, setTenantId] = useState("");
  const [name, setName] = useState("");
  const [isolationTier, setIsolationTier] = useState<"POOLED" | "SILO">("POOLED");
  const [region, setRegion] = useState("id-jkt");

  function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    createTenant.mutate({ tenantId, name, isolationTier, region });
    setTenantId("");
    setName("");
  }

  return (
    <div>
      <h1>Tenants</h1>
      <p>Platform-wide tenant lifecycle management — visible only to <code>platform-admin</code>.</p>

      <form onSubmit={handleCreate} className="card">
        <h2>Create a tenant</h2>
        <label>
          Tenant ID
          <input value={tenantId} onChange={(e) => setTenantId(e.target.value)} required placeholder="e.g. acme-corp" />
        </label>
        <label>
          Name
          <input value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label>
          Isolation tier
          <select value={isolationTier} onChange={(e) => setIsolationTier(e.target.value as "POOLED" | "SILO")}>
            <option value="POOLED">Pooled (shared database)</option>
            <option value="SILO">Silo (dedicated database, provisioned synchronously)</option>
          </select>
        </label>
        <label>
          Region
          <input value={region} onChange={(e) => setRegion(e.target.value)} required />
        </label>
        <button type="submit" disabled={createTenant.isPending}>
          {createTenant.isPending ? (isolationTier === "SILO" ? "Provisioning silo…" : "Creating…") : "Create tenant"}
        </button>
        {createTenant.isError && (
          <p role="alert">{createTenant.error instanceof ApiError ? createTenant.error.message : "Failed to create tenant"}</p>
        )}
      </form>

      <h2>All tenants</h2>
      {isLoading ? (
        <p><span className="spinner" aria-hidden="true" />Loading…</p>
      ) : !tenants || tenants.length === 0 ? (
        <p>No tenants yet.</p>
      ) : (
        <ul className="entity-list">
          {tenants.map((tenant) => (
            <li key={tenant.tenantId} className="card">
              <strong>{tenant.name}</strong> (<code>{tenant.tenantId}</code>)
              <div>{tenant.isolationTier} · {tenant.region} · {tenant.status}</div>
              <div className="entity-actions">
                {tenant.status === "PROVISIONING" && (
                  <button onClick={() => activateTenant.mutate(tenant.tenantId)} disabled={activateTenant.isPending}>
                    Activate
                  </button>
                )}
                {tenant.isolationTier === "POOLED" && tenant.status === "ACTIVE" && (
                  <button
                    onClick={() => migrateTenant.mutate(tenant.tenantId)}
                    disabled={migrateTenant.isPending && migrateTenant.variables === tenant.tenantId}
                  >
                    {migrateTenant.isPending && migrateTenant.variables === tenant.tenantId ? (
                      <>
                        <span className="spinner" aria-hidden="true" />
                        Migrating…
                      </>
                    ) : (
                      "Migrate to Silo"
                    )}
                  </button>
                )}
                {tenant.status !== "OFFBOARDED" && tenant.status !== "MIGRATING" && (
                  <button className="button-danger" onClick={() => offboardTenant.mutate(tenant.tenantId)} disabled={offboardTenant.isPending}>
                    Offboard
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
      <p>
        "Migrate to Silo" moves an existing pooled tenant's data into a brand-new dedicated database — the tenant is
        write-frozen (status <code>MIGRATING</code>, same enforcement as an offboarded tenant) for the duration, and
        only flips to the silo tier once every service has finished copying that tenant's rows across.
      </p>
      {(activateTenant.isError || offboardTenant.isError || migrateTenant.isError) && (
        <p role="alert">
          {(activateTenant.error ?? offboardTenant.error ?? migrateTenant.error) instanceof ApiError
            ? ((activateTenant.error ?? offboardTenant.error ?? migrateTenant.error) as ApiError).message
            : "Action failed"}
        </p>
      )}
    </div>
  );
}

function OwnTenantView({ tenantId }: { tenantId: string | undefined }) {
  const { data: tenant, isLoading, isError, error } = useTenant(tenantId);

  return (
    <div>
      <h1>My Tenant</h1>
      <p>Only a <code>platform-admin</code> can manage tenant lifecycle — this is a read-only view of your own tenant.</p>
      {isLoading ? (
        <p><span className="spinner" aria-hidden="true" />Loading…</p>
      ) : isError ? (
        <p role="alert">{error instanceof ApiError ? error.message : "Failed to load your tenant"}</p>
      ) : tenant ? (
        <TenantSummary tenant={tenant} />
      ) : (
        <p>Tenant not found.</p>
      )}
    </div>
  );
}

function TenantSummary({ tenant }: { tenant: Tenant }) {
  return (
    <div className="card">
      <strong>{tenant.name}</strong> (<code>{tenant.tenantId}</code>)
      <div>{tenant.isolationTier} · {tenant.region} · {tenant.status}</div>
    </div>
  );
}
