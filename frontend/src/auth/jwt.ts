/**
 * `react-oidc-context`'s `user.profile` reflects only the ID token's
 * claims — Keycloak's default "roles" client scope puts `realm_access`
 * on the *access* token, not the ID token. Every backend service already
 * reads roles/tenant_id straight off the access token's own claims
 * (`Jwt.roles()` in `common`), so the frontend does the same thing here
 * rather than relying on the ID token's narrower claim set.
 */
export interface DecodedAccessToken {
  tenant_id?: string;
  realm_access?: { roles: string[] };
  preferred_username?: string;
}

export function decodeAccessToken(accessToken: string): DecodedAccessToken {
  const payload = accessToken.split(".")[1];
  const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
  return JSON.parse(atob(padded));
}

export function rolesFromAccessToken(accessToken: string | undefined): string[] {
  if (!accessToken) return [];
  return decodeAccessToken(accessToken).realm_access?.roles ?? [];
}
