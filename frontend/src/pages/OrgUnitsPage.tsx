import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { useCreateOrgUnit, useKnownOrgUnits } from "../api/orgUnits";
import { rolesFromAccessToken } from "../auth/jwt";
import { ApiError } from "../api/http";

export function OrgUnitsPage() {
  const auth = useAuth();
  const roles = rolesFromAccessToken(auth.user?.access_token);
  const canAuthor = roles.includes("admin") || roles.includes("manager");
  const { data: units } = useKnownOrgUnits();
  const createUnit = useCreateOrgUnit();

  const [name, setName] = useState("");
  const [unitType, setUnitType] = useState("team");
  const [managerUserId, setManagerUserId] = useState("");

  function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    createUnit.mutate({ name, unitType, managerUserId: managerUserId || undefined });
    setName("");
    setManagerUserId("");
  }

  return (
    <div>
      <h1>Org Hierarchy</h1>
      <p>
        Each unit can sit at a different position in several independent hierarchy types at once
        (e.g. <code>reporting-line</code> vs. <code>cost-center</code>) — re-parenting moves a whole
        subtree in one bounded operation.
      </p>

      {canAuthor ? (
        <form onSubmit={handleCreate} className="card">
          <h2>Create an org unit</h2>
          <label>
            Name
            <input value={name} onChange={(e) => setName(e.target.value)} required />
          </label>
          <label>
            Unit type
            <input value={unitType} onChange={(e) => setUnitType(e.target.value)} required />
          </label>
          <label>
            Manager username (optional)
            <input value={managerUserId} onChange={(e) => setManagerUserId(e.target.value)} placeholder="e.g. maya" />
          </label>
          <button type="submit" disabled={createUnit.isPending}>
            {createUnit.isPending ? "Creating…" : "Create org unit"}
          </button>
          <p>New units are created unparented — attach them to a hierarchy from the unit's own page.</p>
          {createUnit.isError && (
            <p role="alert">{createUnit.error instanceof ApiError ? createUnit.error.message : "Failed to create org unit"}</p>
          )}
        </form>
      ) : (
        <p>Only an admin or manager can create org units — you're signed in as a learner.</p>
      )}

      <h2>Units created this session</h2>
      {units.length === 0 ? (
        <p>No org units yet.</p>
      ) : (
        <ul className="entity-list">
          {units.map((unit) => (
            <li key={unit.orgUnitId} className="card">
              <strong>{unit.name}</strong> — {unit.unitType}
              {unit.managerUserId && <div>Manager: {unit.managerUserId}</div>}
              <div className="entity-actions">
                <Link to={`/org-units/${unit.orgUnitId}`}>Open</Link>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
