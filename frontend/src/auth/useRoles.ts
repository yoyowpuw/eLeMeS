import { useMemo } from "react";
import { useAuth } from "react-oidc-context";
import { decodeAccessToken, rolesFromAccessToken } from "./jwt";
import { ROLE } from "./roles";

/**
 * Single source of truth for role/tenant resolution — replaces the same
 * `rolesFromAccessToken(auth.user?.access_token)` call that used to be
 * duplicated across every page component and Layout.tsx.
 */
export function useRoles() {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;

  return useMemo(() => {
    const roles = rolesFromAccessToken(accessToken);
    const tenantId = accessToken ? decodeAccessToken(accessToken).tenant_id : undefined;
    const username = accessToken ? decodeAccessToken(accessToken).preferred_username : undefined;
    return {
      roles,
      tenantId,
      username,
      isManager: roles.includes(ROLE.MANAGER),
      isAdmin: roles.includes(ROLE.ADMIN),
      isPlatformAdmin: roles.includes(ROLE.PLATFORM_ADMIN),
    };
  }, [accessToken]);
}
