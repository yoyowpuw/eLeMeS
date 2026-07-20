package com.elemes.certification

import java.time.Instant
import java.util.UUID

/** The exact byte sequence that gets signed and, later, re-verified — must be reproducible from stored fields alone. */
object CertificatePayload {
    /**
     * `pathId`/`pathVersionId`/`realizedStepCourseIds` are appended only
     * when `pathId` is non-null — a direct-course certificate's payload
     * stays byte-identical to what it was before Ch.21 §7 was implemented
     * (not just "mostly the same with trailing nulls"), so no pre-existing
     * signature is invalidated by this change.
     */
    fun canonical(
        certificateId: UUID,
        tenantId: String,
        enrollmentId: UUID,
        learnerId: String,
        courseId: String,
        contentVersionId: UUID,
        score: Int?,
        issuedAt: Instant,
        pathId: UUID? = null,
        pathVersionId: UUID? = null,
        realizedStepCourseIds: List<UUID>? = null,
    ): String {
        val base = listOf(certificateId, tenantId, enrollmentId, learnerId, courseId, contentVersionId, score, issuedAt)
        val pathFields = if (pathId != null) listOf(pathId, pathVersionId, realizedStepCourseIds?.joinToString(",")) else emptyList()
        return (base + pathFields).joinToString("|")
    }
}
