package com.elemes.certification.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

/**
 * Ch.19: resolves the caller's own org-unit authority scope for
 * manager-scoped revocation. Synchronous, token-relayed — same
 * cross-service pattern as Enrollment's CourseManagementClient, chosen for
 * the same reason: no service-to-service auth story exists yet, so the
 * caller's own bearer token is relayed rather than a separate service
 * credential.
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
