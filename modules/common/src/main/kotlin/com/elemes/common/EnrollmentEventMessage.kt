package com.elemes.common

import java.time.Instant
import java.util.UUID

/**
 * Ch.11 §4 Published Language: Enrollment's wire contract for every committed
 * event, consumed by Certification (on ContentCompleted/GradingPassed) today
 * and available for future consumers (e.g. Notification) without further
 * change to Enrollment itself.
 */
data class EnrollmentEventMessage(
    val eventType: String,
    val enrollmentId: UUID,
    val tenantId: String,
    val learnerId: String,
    val courseId: String,
    val score: Int?,
    val occurredAt: Instant,
)

object EnrollmentEventTopics {
    const val ENROLLMENT_EVENTS = "enrollment-events"
}
