package com.elemes.tenancy.infrastructure

import com.elemes.tenancy.Tenant
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Ch.18 §5: "control-plane access revoked immediately" on offboarding is
 * only true if every other service's authorization check can see the new
 * status the instant it changes — a synchronous HTTP call to this service
 * from every OPA evaluation, on every request, would work but adds a
 * dependency and a round-trip to every single authz decision platform-wide
 * (the same tradeoff Ch.19's org-scope caching exists to avoid). Instead,
 * this pushes the current status straight into OPA's data API
 * (`PUT /v1/data/tenants/<tenantId>`) on every transition — OPA
 * already holds this in memory for every policy evaluation, no matter
 * which service is asking, with zero added latency per request.
 *
 * This does mean OPA's copy is only as fresh as the last successful push —
 * if this call fails, the tenant's status in OPA is stale until the next
 * transition retries it. Best-effort, logged, not retried: acceptable for
 * a local dev stand-in, called out in README as the real gap it is.
 *
 * Gotcha this exact path caught once already: `PUT /v1/data/X/Y` writes to
 * data document path `data.X.Y`, which is completely independent of any
 * Rego `package` declaration — a policy's `package elemes.authz` only
 * controls where that file's own *rules* live (`data.elemes.authz.*`), it
 * has no bearing on what `data.tenants[...]` inside a rule body resolves
 * to. Pushing to `/v1/data/elemes/tenants/...` (matching the package name)
 * while the policy read `data.tenants[...]` (no prefix) meant the policy
 * was always reading an empty, always-undefined path — `tenant_active`
 * silently returned true for every tenant regardless of status, caught by
 * directly querying OPA's raw data document and finding it empty at the
 * path the policy actually reads, not the path this pushes to.
 */
@Component
class OpaDataPusher(@Value("\${opa.base-url}") opaBaseUrl: String) {

    private val restClient = RestClient.create(opaBaseUrl)
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * `isolationTier`/`siloDatabaseUrl` ride along with `status` on the same
     * push — Ch.12 §2 silo tier: every data-plane service's
     * `SiloRoutingClient` reads this same pushed document to decide which
     * physical database a tenant's connections belong on, reusing this
     * mechanism rather than a second registry query path.
     */
    fun push(tenant: Tenant) {
        try {
            restClient.put()
                .uri("/v1/data/tenants/{tenantId}", tenant.tenantId)
                .body(
                    mapOf(
                        "status" to tenant.status.name,
                        "isolationTier" to tenant.isolationTier.name,
                        "siloDatabaseUrl" to tenant.siloDatabase,
                    )
                )
                .retrieve()
                .toBodilessEntity()
        } catch (ex: Exception) {
            log.error("Failed to push tenant {} status {} to OPA — its authz decisions may use a stale status until the next transition", tenant.tenantId, tenant.status, ex)
        }
    }
}
