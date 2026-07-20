package com.elemes.enrollment

import com.elemes.common.EventSourcedAggregate
import com.elemes.common.TenantId
import java.util.UUID

/** Ch.5 §4 state machine, up to Completed — Certified stage arrives with Ch.26 Certification. */
enum class EnrollmentStatus { ASSIGNED, IN_PROGRESS, AWAITING_GRADING, COMPLETED }

class Enrollment private constructor(
    val enrollmentId: UUID,
) : EventSourcedAggregate<EnrollmentEvent>() {

    // `lateinit` isn't permitted on value-class-typed properties in Kotlin,
    // hence the nullable-backing-field pattern here (tenantId is always set by
    // the first event applied, whether via enroll() or rehydrate()).
    private var _tenantId: TenantId? = null
    val tenantId: TenantId get() = _tenantId ?: error("Enrollment $enrollmentId has no events applied yet")

    lateinit var learnerId: String
        private set
    lateinit var courseId: String
        private set
    lateinit var contentVersionId: UUID
        private set
    var orgUnitId: UUID? = null
        private set
    var pathProgressId: UUID? = null
        private set
    var status: EnrollmentStatus = EnrollmentStatus.ASSIGNED
        private set
    var progressPercent: Int = 0
        private set

    companion object {
        fun enroll(
            enrollmentId: UUID,
            tenantId: TenantId,
            learnerId: String,
            courseId: String,
            contentVersionId: UUID,
            orgUnitId: UUID? = null,
            pathProgressId: UUID? = null,
        ): Enrollment {
            val enrollment = Enrollment(enrollmentId)
            enrollment.raise(LearnerEnrolled(enrollmentId, tenantId, learnerId, courseId, contentVersionId, orgUnitId, pathProgressId))
            return enrollment
        }

        fun rehydrate(enrollmentId: UUID, history: List<EnrollmentEvent>): Enrollment {
            val enrollment = Enrollment(enrollmentId)
            enrollment.loadFromHistory(history)
            return enrollment
        }
    }

    fun start() {
        check(status == EnrollmentStatus.ASSIGNED) { "Cannot start enrollment in status $status" }
        raise(ContentStarted(enrollmentId, tenantId))
    }

    fun recordProgress(percent: Int) {
        check(status == EnrollmentStatus.IN_PROGRESS) { "Cannot record progress in status $status" }
        require(percent in 0..100) { "percent must be within 0..100" }
        // Ch.37 ADR-060: highest-progress-wins — a regressive sync (e.g. a stale
        // offline device catching up) must never overwrite real progress already recorded.
        if (percent > progressPercent) {
            raise(ContentProgressed(enrollmentId, tenantId, percent))
        }
    }

    /** No-assessment-required path: `InProgress -> Completed: ContentCompleted` per Ch.5 §4. */
    fun complete() {
        check(status == EnrollmentStatus.IN_PROGRESS) { "Cannot complete enrollment in status $status" }
        raise(ContentCompleted(enrollmentId, tenantId))
    }

    /**
     * Reactions to the Assessment context's published events (Ch.11 §4),
     * consumed over Kafka. Idempotency note: at-least-once Kafka delivery
     * means these can be called more than once for the same upstream event —
     * the `check()` guards below make a duplicate call a harmless no-op for
     * the *caller* to catch, not a silent corruption of state.
     */
    fun enterGrading(assessmentId: UUID) {
        check(status == EnrollmentStatus.IN_PROGRESS) { "Cannot enter grading in status $status" }
        raise(GradingStarted(enrollmentId, tenantId, assessmentId))
    }

    fun passGrading(assessmentId: UUID, score: Int) {
        check(status == EnrollmentStatus.AWAITING_GRADING) { "Cannot pass grading in status $status" }
        raise(GradingPassed(enrollmentId, tenantId, assessmentId, score))
    }

    /** `AwaitingGrading -> InProgress: AssessmentFailed (remediation)` per Ch.5 §4. */
    fun failGrading(assessmentId: UUID, score: Int) {
        check(status == EnrollmentStatus.AWAITING_GRADING) { "Cannot fail grading in status $status" }
        raise(GradingFailed(enrollmentId, tenantId, assessmentId, score))
    }

    override fun apply(event: EnrollmentEvent) {
        when (event) {
            is LearnerEnrolled -> {
                _tenantId = event.tenantId
                learnerId = event.learnerId
                courseId = event.courseId
                contentVersionId = event.contentVersionId
                orgUnitId = event.orgUnitId
                pathProgressId = event.pathProgressId
                status = EnrollmentStatus.ASSIGNED
            }
            is ContentStarted -> status = EnrollmentStatus.IN_PROGRESS
            is ContentProgressed -> progressPercent = event.percentComplete
            is ContentCompleted -> {
                status = EnrollmentStatus.COMPLETED
                progressPercent = 100
            }
            is GradingStarted -> status = EnrollmentStatus.AWAITING_GRADING
            is GradingPassed -> {
                status = EnrollmentStatus.COMPLETED
                progressPercent = 100
            }
            is GradingFailed -> status = EnrollmentStatus.IN_PROGRESS
        }
    }
}
