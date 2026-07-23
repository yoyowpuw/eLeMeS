import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { apiFetch, SERVICE_URLS } from "./http";
import type { OrgUnit } from "./types";

export const DEFAULT_HIERARCHY_TYPE = "reporting-line";

export function useOrgUnit(orgUnitId: string | undefined) {
  const auth = useAuth();
  return useQuery({
    queryKey: ["orgUnit", orgUnitId],
    queryFn: () => apiFetch<OrgUnit>(SERVICE_URLS.orgUnits, `/api/v1/org-units/${orgUnitId}`, auth.user?.access_token),
    enabled: !!orgUnitId && !!auth.user,
  });
}

export function useDescendants(orgUnitId: string | undefined, hierarchyType: string) {
  const auth = useAuth();
  return useQuery({
    queryKey: ["orgUnitDescendants", orgUnitId, hierarchyType],
    queryFn: () =>
      apiFetch<OrgUnit[]>(
        SERVICE_URLS.orgUnits,
        `/api/v1/org-units/${orgUnitId}/descendants?hierarchyType=${encodeURIComponent(hierarchyType)}`,
        auth.user?.access_token,
      ),
    enabled: !!orgUnitId && !!auth.user,
  });
}

export function useMyScope(hierarchyType: string) {
  const auth = useAuth();
  return useQuery({
    queryKey: ["myOrgScope", hierarchyType],
    queryFn: () =>
      apiFetch<string[]>(
        SERVICE_URLS.orgUnits,
        `/api/v1/org-units/my-scope?hierarchyType=${encodeURIComponent(hierarchyType)}`,
        auth.user?.access_token,
      ),
    enabled: !!auth.user,
  });
}

export function useCreateOrgUnit() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: { name: string; unitType: string; managerUserId?: string }) =>
      apiFetch<OrgUnit>(SERVICE_URLS.orgUnits, "/api/v1/org-units", auth.user?.access_token, {
        method: "POST",
        body: JSON.stringify(body),
      }),
    onSuccess: (unit) => {
      queryClient.setQueryData(["knownOrgUnits"], (existing: OrgUnit[] = []) => [...existing, unit]);
    },
  });
}

export function useReparent() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ orgUnitId, newParentId, hierarchyType }: { orgUnitId: string; newParentId: string | null; hierarchyType: string }) =>
      apiFetch<OrgUnit>(SERVICE_URLS.orgUnits, `/api/v1/org-units/${orgUnitId}/reparent`, auth.user?.access_token, {
        method: "POST",
        body: JSON.stringify({ newParentId, hierarchyType }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["orgUnitDescendants"] });
    },
  });
}

/** Same session-scoped-list approach used across this app — org-hierarchy has no "list all units" endpoint. */
export function useKnownOrgUnits() {
  return useQuery<OrgUnit[]>({
    queryKey: ["knownOrgUnits"],
    queryFn: () => Promise.resolve([]),
    staleTime: Infinity,
    initialData: [],
  });
}
