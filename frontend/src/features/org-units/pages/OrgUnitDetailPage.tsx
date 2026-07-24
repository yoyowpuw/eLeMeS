import { useState } from "react";
import { useParams } from "react-router-dom";
import { DEFAULT_HIERARCHY_TYPE, useDescendants, useKnownOrgUnits, useOrgUnit, useReparent } from "../../../api/orgUnits";
import { ApiError } from "../../../api/http";
import { useRoles } from "../../../auth/useRoles";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { Button } from "../../../components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../../components/ui/select";
import { Skeleton } from "../../../components/ui/skeleton";

const ROOT = "__root__";

export function OrgUnitDetailPage() {
  const { orgUnitId } = useParams<{ orgUnitId: string }>();
  const { isManager, isAdmin } = useRoles();
  const canAuthor = isManager || isAdmin;

  const [hierarchyType, setHierarchyType] = useState(DEFAULT_HIERARCHY_TYPE);
  const { data: unit, isLoading } = useOrgUnit(orgUnitId);
  const { data: descendants } = useDescendants(orgUnitId, hierarchyType);
  const { data: knownUnits } = useKnownOrgUnits();
  const reparent = useReparent();

  const [newParentId, setNewParentId] = useState(ROOT);
  const [reparentHierarchyType, setReparentHierarchyType] = useState(DEFAULT_HIERARCHY_TYPE);

  if (isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full max-w-lg" />
      </div>
    );
  }
  if (!unit) return <p className="text-sm text-slate-500">Org unit not found.</p>;

  function handleReparent(e: React.FormEvent) {
    e.preventDefault();
    if (!orgUnitId) return;
    reparent.mutate({ orgUnitId, newParentId: newParentId === ROOT ? null : newParentId, hierarchyType: reparentHierarchyType });
  }

  const otherKnownUnits = knownUnits.filter((u) => u.orgUnitId !== orgUnitId);

  return (
    <div className="flex max-w-lg flex-col gap-4">
      <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">{unit.name}</h1>

      <Card>
        <CardContent className="flex flex-col gap-1 pt-4 text-sm">
          <div>Type: {unit.unitType}</div>
          {unit.managerUserId && <div>Manager: {unit.managerUserId}</div>}
          <code className="text-xs text-slate-500 dark:text-slate-400">{unit.orgUnitId}</code>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Descendants</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="hierarchy-type">Hierarchy type</Label>
            <Input id="hierarchy-type" value={hierarchyType} onChange={(e) => setHierarchyType(e.target.value)} />
          </div>
          {descendants && descendants.length > 0 ? (
            <ul className="flex flex-col gap-1 text-sm">
              {descendants.map((d) => (
                <li key={d.orgUnitId}>
                  {d.name} <span className="text-slate-500 dark:text-slate-400">({d.unitType})</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-slate-500 dark:text-slate-400">No descendants under "{hierarchyType}".</p>
          )}
        </CardContent>
      </Card>

      {canAuthor && (
        <Card>
          <CardHeader>
            <CardTitle>Re-parent this unit</CardTitle>
          </CardHeader>
          <form onSubmit={handleReparent}>
            <CardContent className="flex flex-col gap-3">
              <div className="flex flex-col gap-1.5">
                <Label>New parent</Label>
                <Select value={newParentId} onValueChange={setNewParentId}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={ROOT}>— none (detach to root) —</SelectItem>
                    {otherKnownUnits.map((u) => (
                      <SelectItem key={u.orgUnitId} value={u.orgUnitId}>
                        {u.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="reparent-hierarchy-type">Hierarchy type</Label>
                <Input id="reparent-hierarchy-type" value={reparentHierarchyType} onChange={(e) => setReparentHierarchyType(e.target.value)} required />
              </div>
              <Button type="submit" isLoading={reparent.isPending} className="self-start">
                {reparent.isPending ? "Re-parenting…" : "Re-parent"}
              </Button>
              <p className="text-xs text-slate-500 dark:text-slate-400">Only rewrites this subtree's rows for this one hierarchy type — every other hierarchy type this unit belongs to is untouched.</p>
              {reparent.isError && (
                <p role="alert" className="text-sm text-red-600">
                  {reparent.error instanceof ApiError ? reparent.error.message : "Failed to re-parent — you may be outside this unit's org scope"}
                </p>
              )}
            </CardContent>
          </form>
        </Card>
      )}
    </div>
  );
}
