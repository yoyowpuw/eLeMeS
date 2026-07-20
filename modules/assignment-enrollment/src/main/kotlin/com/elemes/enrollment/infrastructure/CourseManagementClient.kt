package com.elemes.enrollment.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.util.UUID

data class CourseVersionInfo(val versionId: UUID, val courseId: UUID, val versionNumber: Int, val contentHash: String)

/**
 * Ch.11 §3/§4: Course Management -> Assignment & Enrollment is a
 * Customer-Supplier relationship, called synchronously because enrollment
 * needs a read-your-writes guarantee both that the course exists AND which
 * content version is current *right now* — that version gets pinned into
 * the Enrollment aggregate (Ch.5 ADR-005, Ch.21 §7) and never re-queried.
 *
 * Ch.16: Course Management now requires a valid token on every request, so
 * the caller's own bearer token is relayed here (token-relay pattern) rather
 * than this service having its own service-account credentials — simplest
 * correct choice while there's no service-to-service auth story yet.
 */
@Component
class CourseManagementClient(@Value("\${course-management.base-url}") baseUrl: String) {

    private val restClient = RestClient.create(baseUrl)

    fun getCurrentVersion(courseId: String, bearerToken: String): CourseVersionInfo? = try {
        restClient.get()
            .uri("/api/v1/courses/{id}/current-version", courseId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $bearerToken")
            .retrieve()
            .body(CourseVersionInfo::class.java)
    } catch (ex: RestClientResponseException) {
        if (ex.statusCode.value() == 404) null else throw ex
    }
}
