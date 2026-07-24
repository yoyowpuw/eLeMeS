import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useRoles } from "../../auth/useRoles";
import type { Role } from "../../auth/roles";
import { RequireAuth } from "./RequireAuth";

/**
 * Ch.4 ADR-004 / Ch.14 §4-5: Manager and Admin surfaces must be structurally
 * separate route trees, not a single UI with permission-gated nav items — a
 * caller lacking any of `role` gets redirected away entirely (with the
 * reason carried in navigation state for the destination to surface as a
 * toast), not shown a degraded render of the same route. This is the
 * route-level counterpart to the backend's own independent authorization
 * check (Ch.17) — a UI convenience on top of a real boundary, not the
 * boundary itself.
 *
 * `role` accepts an array because `platform-admin` (tenant-provisioning's
 * own platform-wide identity, tenant_id "platform") is a genuinely
 * different role than a business tenant's `admin` — `platform-ops`'s JWT
 * never carries `admin`. Gating `/admin` on `admin` alone would lock
 * platform-admin out of tenant management entirely, a real regression from
 * the old flat-route app where `/tenants` only required being signed in.
 */
export function RequireRole({ role, children }: { role: Role | Role[]; children: ReactNode }) {
  const { roles } = useRoles();
  const required = Array.isArray(role) ? role : [role];
  const allowed = required.some((r) => roles.includes(r));

  return (
    <RequireAuth>
      {allowed ? children : <Navigate to="/" replace state={{ deniedRole: required.join(" or ") }} />}
    </RequireAuth>
  );
}
