package com.elemes.certification

import java.time.Instant
import java.util.UUID

/** The exact byte sequence that gets signed and, later, re-verified — must be reproducible from stored fields alone. */
object CertificatePayload {
    fun canonical(
        certificateId: UUID,
        tenantId: String,
        enrollmentId: UUID,
        learnerId: String,
        courseId: String,
        score: Int?,
        issuedAt: Instant,
    ): String = listOf(certificateId, tenantId, enrollmentId, learnerId, courseId, score, issuedAt).joinToString("|")
}
