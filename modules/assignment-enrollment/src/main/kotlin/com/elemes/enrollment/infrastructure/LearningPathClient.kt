package com.elemes.enrollment.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.util.UUID

data class PathStepInfo(val stepOrder: Int, val courseId: UUID)
data class PathVersionInfo(val pathId: UUID, val versionId: UUID, val versionNumber: Int, val steps: List<PathStepInfo>)

/**
 * Same Customer-Supplier / token-relay shape as CourseManagementClient (see
 * its doc comment) — a separate client class because it targets a distinct
 * resource (learning-paths, not courses), even though it shares
 * course-management's base URL.
 */
@Component
class LearningPathClient(@Value("\${course-management.base-url}") baseUrl: String) {

    private val restClient = RestClient.create(baseUrl)

    fun getCurrentVersion(pathId: UUID, bearerToken: String): PathVersionInfo? = try {
        restClient.get()
            .uri("/api/v1/learning-paths/{id}/current-version", pathId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $bearerToken")
            .retrieve()
            .body(PathVersionInfo::class.java)
    } catch (ex: RestClientResponseException) {
        if (ex.statusCode.value() == 404) null else throw ex
    }
}
