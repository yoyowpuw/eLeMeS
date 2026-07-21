import type { AuthProviderProps } from "react-oidc-context";

/**
 * Ch.16 ADR-026: real OIDC Authorization Code + PKCE against Keycloak — the
 * same identity provider every backend service already validates tokens
 * from, just reached via a browser redirect instead of a password grant
 * (ROPC, used only by curl/scripts elsewhere in this project, is not
 * appropriate for a SPA). Keycloak's `elemes-service` client has
 * `standardFlowEnabled: true` and PKCE (S256) required specifically for
 * this flow — see infra/keycloak/realm-export.json.
 */
export const oidcConfig: AuthProviderProps = {
  authority: "http://localhost:8080/realms/elemes",
  client_id: "elemes-service",
  redirect_uri: window.location.origin,
  post_logout_redirect_uri: window.location.origin,
  scope: "openid profile email",
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};
