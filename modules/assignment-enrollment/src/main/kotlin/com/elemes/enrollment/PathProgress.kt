package com.elemes.enrollment

import java.time.Instant
import java.util.UUID

enum class PathProgressStatus { IN_PROGRESS, COMPLETED }

/**
 * courseId + the content version pinned for it — resolved once, for every
 * step, at path-enrollment time (see PathProgress.stepPlan's doc comment).
 */
data class PathStepPlan(val courseId: UUID, val contentVersionId: UUID)

/**
 * Ch.21 §2: "a read-model projection off Enrollment events, not a duplicate
 * source of truth" — this table is driven entirely by PathProgressService
 * reacting to this same service's own Enrollment completions; it has no
 * independent state-transition logic of its own the way Enrollment does,
 * so it's a plain data class + JDBC repository, not an event-sourced
 * aggregate.
 */
data class PathProgress(
    val pathProgressId: UUID,
    val tenantId: String,
    val learnerId: String,
    val pathId: UUID,
    /** Ch.21 §3 / ADR-034: pinned once at path-enrollment time, same as Enrollment.contentVersionId. */
    val pathVersionId: UUID,
    /**
     * Every step's courseId + content version, resolved once at
     * path-enrollment time (arguably a *stronger* reading of "pinned at
     * enrollment time" than resolving step-by-step: a learner still on step
     * 1 is unaffected by a course update mid-path either way). This is also
     * what makes step advancement — triggered from a Kafka consumer thread
     * with no user token to relay — a pure in-process operation: no
     * synchronous HTTP call back to course-management is ever needed after
     * this is captured, only at path-enrollment time itself, which always
     * has a real caller token.
     */
    val stepPlan: List<PathStepPlan>,
    val currentStepIndex: Int,
    val status: PathProgressStatus,
    /** Course IDs completed so far, in completion order — becomes the certificate's realized-step-sequence claim once the path finishes (Ch.21 §7 / Ch.26 Blue Team addendum). */
    val realizedStepCourseIds: List<UUID>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
