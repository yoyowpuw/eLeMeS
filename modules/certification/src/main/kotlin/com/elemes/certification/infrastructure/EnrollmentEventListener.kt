package com.elemes.certification.infrastructure

import com.elemes.certification.Certificate
import com.elemes.certification.CertificatePayload
import com.elemes.common.EnrollmentEventMessage
import com.elemes.common.EnrollmentEventTopics
import com.elemes.common.TenantContext
import com.elemes.common.TenantId
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/**
 * Ch.5 §3.6 / §4: `Completed -> Certified: CertificateIssued`. Triggers on
 * either completion path from Enrollment — ContentCompleted (no assessment
 * required) or GradingPassed (assessment-gated) — both of which mean the
 * same thing from Certification's point of view: the enrollment is done.
 */
@Component
class EnrollmentEventListener(
    private val repository: CertificateRepository,
    private val signingService: LocalSigningService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val completionEventTypes = setOf("ContentCompleted", "GradingPassed")

    @KafkaListener(topics = [EnrollmentEventTopics.ENROLLMENT_EVENTS])
    fun onEnrollmentEvent(message: EnrollmentEventMessage) {
        if (message.eventType !in completionEventTypes) return

        // Ch.12 §2: this runs on a Kafka consumer thread, not an HTTP request
        // thread, so TenantContextFilter never ran — there's no JWT to read
        // tenant_id from here, only the message's own field. Must be set
        // before any DB access below, or Postgres RLS blocks all of it.
        TenantContext.set(message.tenantId)
        try {
            // Idempotency guard: at-least-once delivery + the unique constraint on
            // certificate_projection.enrollment_id both protect against issuing
            // two certificates for one enrollment.
            if (repository.findByEnrollmentId(message.enrollmentId) != null) {
                log.info("Certificate already exists for enrollment {}, ignoring duplicate completion event", message.enrollmentId)
                return
            }

            val certificateId = UUID.randomUUID()
            val issuedAt = Instant.now()
            val payload = CertificatePayload.canonical(
                certificateId, message.tenantId, message.enrollmentId, message.learnerId,
                message.courseId, message.contentVersionId, message.score, issuedAt,
            )
            val signature = signingService.sign(payload)

            val certificate = Certificate.issue(
                certificateId = certificateId,
                tenantId = TenantId(message.tenantId),
                enrollmentId = message.enrollmentId,
                learnerId = message.learnerId,
                courseId = message.courseId,
                contentVersionId = message.contentVersionId,
                orgUnitId = message.orgUnitId,
                score = message.score,
                signature = signature,
                issuedAt = issuedAt,
            )
            repository.save(certificate)
            log.info("Issued certificate {} for enrollment {} (trigger: {})", certificateId, message.enrollmentId, message.eventType)
        } finally {
            TenantContext.clear()
        }
    }
}
