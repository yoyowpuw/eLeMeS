package com.elemes.course.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

/**
 * Ch.19: resolves the caller's own org-unit authority scope for
 * manager-scoped course creation/publishing. Same token-relayed,
 * synchronous cross-service pattern as Certification's client of the same
 * name — no service-to-service auth story exists yet.
 */
@Component
class OrgHierarchyClient(@Value("\${org-hierarchy.base-url}") baseUrl: String) {

    private val restClient = RestClient.create(baseUrl)

    fun myScope(bearerToken: String, hierarchyType: String = "reporting-line"): List<UUID> =
        restClient.get()
            .uri("/api/v1/org-units/my-scope?hierarchyType={hierarchyType}", hierarchyType)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $bearerToken")
            .retrieve()
            .body(Array<UUID>::class.java)
            ?.toList() ?: emptyList()
}
