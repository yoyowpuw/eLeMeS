import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { apiFetch, SERVICE_URLS } from "./http";
import type { Tenant } from "./types";

/**
 * platform-admin only — a tenant's own admin can only read their own tenant
 * via useTenant(). `enabled` must be gated by the caller on the platform-admin
 * role, since a non-platform-admin calling this endpoint gets a 403.
 */
export function useTenants(enabled: boolean) {
  const auth = useAuth();
  return useQuery({
    queryKey: ["tenants"],
    queryFn: () => apiFetch<Tenant[]>(SERVICE_URLS.tenants, "/api/v1/tenants", auth.user?.access_token),
    enabled: enabled && !!auth.user,
  });
}

export function useTenant(tenantId: string | undefined) {
  const auth = useAuth();
  return useQuery({
    queryKey: ["tenant", tenantId],
    queryFn: () => apiFetch<Tenant>(SERVICE_URLS.tenants, `/api/v1/tenants/${tenantId}`, auth.user?.access_token),
    enabled: !!tenantId && !!auth.user,
  });
}

export function useCreateTenant() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: { tenantId: string; name: string; isolationTier: "POOLED" | "SILO"; region: string }) =>
      apiFetch<Tenant>(SERVICE_URLS.tenants, "/api/v1/tenants", auth.user?.access_token, {
        method: "POST",
        body: JSON.stringify(body),
      }),
    onSuccess: (tenant) => {
      queryClient.setQueryData(["tenants"], (existing: Tenant[] = []) => [...existing, tenant]);
    },
  });
}

export function useActivateTenant() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (tenantId: string) =>
      apiFetch<Tenant>(SERVICE_URLS.tenants, `/api/v1/tenants/${tenantId}/activate`, auth.user?.access_token, {
        method: "POST",
      }),
    onSuccess: (tenant) => {
      queryClient.setQueryData(["tenants"], (existing: Tenant[] = []) => existing.map((t) => (t.tenantId === tenant.tenantId ? tenant : t)));
      queryClient.setQueryData(["tenant", tenant.tenantId], tenant);
    },
  });
}

/** Ch.12 §2 pool-to-silo migration — synchronous like SILO-at-creation, so this can take a few seconds (schema provisioning + a real data copy across all 5 services) before it resolves. */
export function useMigrateTenantToSilo() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (tenantId: string) =>
      apiFetch<Tenant>(SERVICE_URLS.tenants, `/api/v1/tenants/${tenantId}/migrate-to-silo`, auth.user?.access_token, {
        method: "POST",
      }),
    onSuccess: (tenant) => {
      queryClient.setQueryData(["tenants"], (existing: Tenant[] = []) => existing.map((t) => (t.tenantId === tenant.tenantId ? tenant : t)));
      queryClient.setQueryData(["tenant", tenant.tenantId], tenant);
    },
  });
}

export function useOffboardTenant() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (tenantId: string) =>
      apiFetch<Tenant>(SERVICE_URLS.tenants, `/api/v1/tenants/${tenantId}/offboard`, auth.user?.access_token, {
        method: "POST",
      }),
    onSuccess: (tenant) => {
      queryClient.setQueryData(["tenants"], (existing: Tenant[] = []) => existing.map((t) => (t.tenantId === tenant.tenantId ? tenant : t)));
      queryClient.setQueryData(["tenant", tenant.tenantId], tenant);
    },
  });
}
