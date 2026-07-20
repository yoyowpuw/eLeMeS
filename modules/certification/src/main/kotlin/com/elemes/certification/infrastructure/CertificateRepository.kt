package com.elemes.certification.infrastructure

import com.elemes.certification.Certificate
import com.elemes.common.EventStore
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class CertificateRepository(
    private val eventStore: EventStore,
    private val mapper: CertificateEventMapper,
    private val jdbcTemplate: JdbcTemplate,
) {
    fun findById(certificateId: UUID): Certificate? {
        val envelopes = eventStore.loadEvents(certificateId)
        if (envelopes.isEmpty()) return null
        return Certificate.rehydrate(certificateId, envelopes.map(mapper::toDomainEvent))
    }

    /** The idempotency guard consumed by the Kafka listener: one certificate per enrollment, ever. */
    fun findByEnrollmentId(enrollmentId: UUID): UUID? =
        jdbcTemplate.query(
            "select certificate_id from certificate_projection where enrollment_id = ?",
            { rs, _ -> UUID.fromString(rs.getString("certificate_id")) },
            enrollmentId,
        ).firstOrNull()

    @Transactional
    fun save(certificate: Certificate) {
        val uncommitted = certificate.uncommittedEvents
        if (uncommitted.isEmpty()) return

        val baseVersion = certificate.version - uncommitted.size
        eventStore.append(certificate.certificateId, "Certificate", baseVersion, uncommitted.map(mapper::toEnvelope))
        updateProjection(certificate)
        certificate.markCommitted()
    }

    private fun updateProjection(certificate: Certificate) {
        jdbcTemplate.update(
            """
            insert into certificate_projection
                (certificate_id, tenant_id, enrollment_id, learner_id, course_id, score, signature, status, issued_at, updated_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (certificate_id) do update set
                status = excluded.status,
                updated_at = excluded.updated_at
            """.trimIndent(),
            certificate.certificateId,
            certificate.tenantId.value,
            certificate.enrollmentId,
            certificate.learnerId,
            certificate.courseId,
            certificate.score,
            certificate.signature,
            certificate.status.name,
            Timestamp.from(certificate.issuedAt),
            Timestamp.from(Instant.now()),
        )
    }
}
