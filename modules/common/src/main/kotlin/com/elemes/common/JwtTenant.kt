package com.elemes.common

import org.springframework.security.oauth2.jwt.Jwt

/**
 * Ch.16 ADR-026 (bought CIAM, Keycloak stand-in locally): every access token
 * carries a `tenant_id` claim (see infra/keycloak/realm-export.json's
 * protocol mapper) — this is the one, single place that claim is read, so
 * every service extracts tenant identity identically.
 */
fun Jwt.tenantId(): TenantId =
    TenantId(getClaimAsString("tenant_id") ?: error("JWT is missing the required tenant_id claim"))

/** Keycloak nests realm roles under `realm_access.roles`, not a flat claim — this is the one place that's unpacked. */
@Suppress("UNCHECKED_CAST")
fun Jwt.roles(): List<String> {
    val realmAccess = getClaim<Map<String, Any>>("realm_access") ?: return emptyList()
    return (realmAccess["roles"] as? List<String>) ?: emptyList()
}
