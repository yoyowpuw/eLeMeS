package com.elemes.common

import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class TenantRouting(val isolationTier: String, val siloDatabaseUrl: String?)

private val POOLED_ROUTING = TenantRouting("POOLED", null)

/**
 * Ch.12 §2 silo tier: resolves whether a tenant's connections should go to
 * the pooled cluster or its own dedicated database, by reading the same
 * `data.tenants.<id>` document `OpaDataPusher` already writes on every
 * tenant lifecycle transition (extended to also carry `isolationTier` and
 * `siloDatabaseUrl`) — reusing the existing pushed-registry mechanism
 * (README "What's proven" #16) rather than inventing a second one.
 *
 * Cached per tenantId with no expiry, deliberately: unlike tenant *status*
 * (which changes and is re-checked on every authz decision), a tenant's
 * isolation tier and silo database are fixed forever at provisioning
 * time — the AKB itself (Ch.18 §6 CTO note) flags pool-to-silo migration as
 * an unimplemented, explicitly out-of-scope follow-up, so there is no
 * "changed after first resolution" case to handle yet. An unknown tenant
 * (not yet pushed, or OPA unreachable) resolves to pooled — the safe
 * default, since the pooled cluster's RLS already isolates it correctly.
 */
class SiloRoutingClient(opaBaseUrl: String) {

    private val restClient = RestClient.create(opaBaseUrl)
    private val log = LoggerFactory.getLogger(javaClass)
    private val cache = ConcurrentHashMap<String, TenantRouting>()

    fun resolve(tenantId: String): TenantRouting = cache.computeIfAbsent(tenantId, ::fetch)

    private val knownSiloTenantsTtl = Duration.ofSeconds(30)
    @Volatile private var knownSiloTenantsCachedAt: Instant = Instant.EPOCH
    @Volatile private var knownSiloTenantsCache: List<String> = emptyList()

    /**
     * [OutboxPoller] needs this — it runs on a `@Scheduled` background
     * thread with no per-request [TenantContext], so unlike every other
     * caller here it can't just resolve one already-known tenant; it has
     * to discover every SILO tenant that might have unpublished outbox
     * rows sitting in a database the pooled-cluster poll would never see.
     * Short TTL (unlike [resolve]'s permanent cache) specifically so a
     * newly-provisioned silo tenant's outbox starts being polled within
     * one cache window, not only after a service restart.
     */
    fun knownSiloTenantIds(): List<String> {
        if (Duration.between(knownSiloTenantsCachedAt, Instant.now()) < knownSiloTenantsTtl) return knownSiloTenantsCache
        return try {
            @Suppress("UNCHECKED_CAST")
            val body = restClient.get().uri("/v1/data/tenants").retrieve().body(Map::class.java) as Map<String, Any?>?
            val allTenants = body?.get("result") as? Map<*, *> ?: emptyMap<Any, Any>()
            val siloIds = allTenants.entries
                .filter { (it.value as? Map<*, *>)?.get("isolationTier") == "SILO" }
                .map { it.key as String }
            knownSiloTenantsCache = siloIds
            knownSiloTenantsCachedAt = Instant.now()
            siloIds
        } catch (ex: Exception) {
            log.warn("Could not list known silo tenants — outbox polling will only cover the pooled cluster this cycle", ex)
            knownSiloTenantsCache
        }
    }

    private fun fetch(tenantId: String): TenantRouting = try {
        @Suppress("UNCHECKED_CAST")
        val body = restClient.get()
            .uri("/v1/data/tenants/{tenantId}", tenantId)
            .retrieve()
            .body(Map::class.java) as Map<String, Any?>?
        val result = body?.get("result") as? Map<*, *>
        val isolationTier = result?.get("isolationTier") as? String ?: "POOLED"
        val siloDatabaseUrl = result?.get("siloDatabaseUrl") as? String
        if (isolationTier == "SILO" && siloDatabaseUrl != null) TenantRouting(isolationTier, siloDatabaseUrl) else POOLED_ROUTING
    } catch (ex: Exception) {
        log.warn("Could not resolve silo routing for tenant {} — defaulting to pooled", tenantId, ex)
        POOLED_ROUTING
    }
}
