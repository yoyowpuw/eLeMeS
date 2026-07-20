package com.elemes.certification.infrastructure

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Thrown when a live org-hierarchy lookup fails AND there is no cached answer to fall back to — deny closed, never fail open on an authorization decision. */
class OrgScopeUnavailableException(message: String) : RuntimeException(message)

/**
 * Ch.19 ADR-032: a 5-minute TTL local cache for org-scope lookups,
 * refreshed by `OrgUnitEventListener` on any `OrgUnitChanged`/
 * `OrgUnitReparented` event rather than polling.
 *
 * ADR-032 was written for Assignment's *eligibility computation* on the
 * compliance-critical golden path, where "never block, use last-known-good
 * on Org Hierarchy outage" is explicitly the right call — a stale
 * eligibility answer is a UX/data-freshness problem, not a security one.
 * This cache serves a different kind of read: an *authorization* decision
 * (can this manager revoke this certificate). Using a stale-but-known-good
 * cached scope on a transient outage is still fine — the cache was correct
 * moments ago and gets invalidated the instant something actually changes.
 * But unlike ADR-032's original context, a cache MISS on outage must never
 * fail open: there is no safe "assume authorized" default for a security
 * check, so a miss with no fallback throws [OrgScopeUnavailableException]
 * instead of silently granting or silently denying.
 */
@Component
class OrgScopeCache(private val client: OrgHierarchyClient) {

    private data class Entry(val scope: List<String>, val cachedAt: Instant)

    private val ttl: Duration = Duration.ofMinutes(5)
    private val cache = ConcurrentHashMap<String, Entry>()
    private val log = LoggerFactory.getLogger(javaClass)

    fun getScope(cacheKey: String, bearerToken: String, hierarchyType: String = "reporting-line"): List<String> {
        val existing = cache[cacheKey]
        if (existing != null && Duration.between(existing.cachedAt, Instant.now()) < ttl) {
            return existing.scope
        }
        return try {
            val fresh = client.myScope(bearerToken, hierarchyType).map { it.toString() }
            cache[cacheKey] = Entry(fresh, Instant.now())
            fresh
        } catch (ex: Exception) {
            if (existing != null) {
                log.warn("org-hierarchy unreachable, falling back to last-known-good scope for {}", cacheKey, ex)
                existing.scope
            } else {
                throw OrgScopeUnavailableException("Cannot resolve org scope for '$cacheKey' — org-hierarchy is unreachable and no cached answer exists")
            }
        }
    }

    /** Invalidated wholesale on any org-unit event — coarse but always correct, unlike a per-manager invalidation that would need to know exactly who's affected. */
    fun invalidateAll() {
        cache.clear()
    }
}
