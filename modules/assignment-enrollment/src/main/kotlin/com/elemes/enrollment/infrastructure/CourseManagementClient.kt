package com.elemes.enrollment.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

/**
 * Ch.11 §3/§4: Course Management -> Assignment & Enrollment is a
 * Customer-Supplier relationship, called synchronously here because
 * enrollment needs a read-your-writes guarantee that the course actually
 * exists before committing an Enrollment aggregate.
 */
@Component
class CourseManagementClient(@Value("\${course-management.base-url}") baseUrl: String) {

    private val restClient = RestClient.create(baseUrl)

    fun courseExists(courseId: String): Boolean = try {
        restClient.get().uri("/api/v1/courses/{id}", courseId).retrieve().toBodilessEntity()
        true
    } catch (ex: RestClientResponseException) {
        if (ex.statusCode.value() == 404) false else throw ex
    }
}
