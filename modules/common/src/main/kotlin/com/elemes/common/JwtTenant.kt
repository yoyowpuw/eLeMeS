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
