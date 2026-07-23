import { useState } from "react";
import { useParams } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { DEFAULT_HIERARCHY_TYPE, useDescendants, useKnownOrgUnits, useOrgUnit, useReparent } from "../api/orgUnits";
import { rolesFromAccessToken } from "../auth/jwt";
import { ApiError } from "../api/http";

const ROOT = "__root__";

export function OrgUnitDetailPage() {
  const { orgUnitId } = useParams<{ orgUnitId: string }>();
  const auth = useAuth();
  const roles = rolesFromAccessToken(auth.user?.access_token);
  const canAuthor = roles.includes("admin") || roles.includes("manager");

  const [hierarchyType, setHierarchyType] = useState(DEFAULT_HIERARCHY_TYPE);
  const { data: unit, isLoading } = useOrgUnit(orgUnitId);
  const { data: descendants } = useDescendants(orgUnitId, hierarchyType);
  const { data: knownUnits } = useKnownOrgUnits();
  const reparent = useReparent();

  const [newParentId, setNewParentId] = useState(ROOT);
  const [reparentHierarchyType, setReparentHierarchyType] = useState(DEFAULT_HIERARCHY_TYPE);

  if (isLoading) return <p>Loading…</p>;
  if (!unit) return <p>Org unit not found.</p>;

  function handleReparent(e: React.FormEvent) {
    e.preventDefault();
    if (!orgUnitId) return;
    reparent.mutate({ orgUnitId, newParentId: newParentId === ROOT ? null : newParentId, hierarchyType: reparentHierarchyType });
  }

  const otherKnownUnits = knownUnits.filter((u) => u.orgUnitId !== orgUnitId);

  return (
    <div>
      <h1>{unit.name}</h1>
      <div className="card">
        <div>Type: {unit.unitType}</div>
        {unit.managerUserId && <div>Manager: {unit.managerUserId}</div>}
        <div><code>{unit.orgUnitId}</code></div>
      </div>

      <div className="card">
        <h2>Descendants</h2>
        <label>
          Hierarchy type
          <input value={hierarchyType} onChange={(e) => setHierarchyType(e.target.value)} />
        </label>
        {descendants && descendants.length > 0 ? (
          <ul>
            {descendants.map((d) => (
              <li key={d.orgUnitId}>{d.name} ({d.unitType})</li>
            ))}
          </ul>
        ) : (
          <p>No descendants under "{hierarchyType}".</p>
        )}
      </div>

      {canAuthor && (
        <form onSubmit={handleReparent} className="card">
          <h2>Re-parent this unit</h2>
          <label>
            New parent
            <select value={newParentId} onChange={(e) => setNewParentId(e.target.value)}>
              <option value={ROOT}>— none (detach to root) —</option>
              {otherKnownUnits.map((u) => (
                <option key={u.orgUnitId} value={u.orgUnitId}>{u.name}</option>
              ))}
            </select>
          </label>
          <label>
            Hierarchy type
            <input value={reparentHierarchyType} onChange={(e) => setReparentHierarchyType(e.target.value)} required />
          </label>
          <button type="submit" disabled={reparent.isPending}>
            {reparent.isPending ? "Re-parenting…" : "Re-parent"}
          </button>
          <p>Only rewrites this subtree's rows for this one hierarchy type — every other hierarchy type this unit belongs to is untouched.</p>
          {reparent.isError && (
            <p role="alert">
              {reparent.error instanceof ApiError ? reparent.error.message : "Failed to re-parent — you may be outside this unit's org scope"}
            </p>
          )}
        </form>
      )}
    </div>
  );
}
