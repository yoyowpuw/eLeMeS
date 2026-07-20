package com.elemes.course.infrastructure

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Thrown when a live org-hierarchy lookup fails AND there is no cached answer to fall back to — deny closed, never fail open on an authorization decision. */
class OrgScopeUnavailableException(message: String) : RuntimeException(message)

/**
 * Ch.19 ADR-032: same 5-minute TTL cache as Certification's `OrgScopeCache`
 * — see that class's doc comment for the full reasoning on why a cache
 * MISS during an org-hierarchy outage fails closed (503) rather than
 * following ADR-032's literal "never block" framing, which was written for
 * eligibility computation, not an authorization decision.
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
