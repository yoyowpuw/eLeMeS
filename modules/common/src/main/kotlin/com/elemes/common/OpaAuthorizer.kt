package com.elemes.common

import org.springframework.web.client.RestClient

data class AuthzInput(
    val action: String,
    val callerTenant: String,
    val callerRoles: List<String>,
    /** Null for actions that create a brand-new resource — there's nothing to tenant-check yet. */
    val resourceTenant: String? = null,
    /** Ch.19: the org units (managed unit + all its descendants) the caller has authority over. Only resolved for org-scoped actions — empty otherwise. */
    val callerOrgUnits: List<String> = emptyList(),
    /** Ch.19: the resource's own org unit, if it has one — null for resources never assigned one, or actions not org-scoped. */
    val resourceOrgUnit: String? = null,
)

private data class OpaRequest(val input: OpaRequestInput)
private data class OpaRequestInput(
    val action: String,
    val caller_tenant: String,
    val caller_roles: List<String>,
    val resource_tenant: String?,
    val caller_org_units: List<String>,
    val resource_org_unit: String?,
)
private data class OpaResponse(val result: Boolean?)

class ForbiddenException(message: String) : RuntimeException(message)

/**
 * Ch.17 ADR-028: queries the OPA sidecar (locally, one shared instance —
 * see infra/opa/policies/authz.rego) for every authorization decision.
 * Deny-by-default: any OPA error, timeout, or missing `result` denies the
 * action rather than failing open.
 */
class OpaAuthorizer(private val opaBaseUrl: String) {

    private val restClient = RestClient.create(opaBaseUrl)

    fun check(input: AuthzInput) {
        val allowed = try {
            restClient.post()
                .uri("/v1/data/elemes/authz/allow")
                .body(
                    OpaRequest(
                        OpaRequestInput(
                            input.action, input.callerTenant, input.callerRoles, input.resourceTenant,
                            input.callerOrgUnits, input.resourceOrgUnit,
                        )
                    )
                )
                .retrieve()
                .body(OpaResponse::class.java)
                ?.result ?: false
        } catch (ex: Exception) {
            false
        }
        if (!allowed) {
            throw ForbiddenException("Not authorized to perform '${input.action}'")
        }
    }
}
