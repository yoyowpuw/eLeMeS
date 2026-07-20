package com.elemes.enrollment.infrastructure

import com.elemes.common.TenantId
import com.elemes.enrollment.Enrollment
import com.elemes.enrollment.PathProgress
import com.elemes.enrollment.PathProgressStatus
import com.elemes.enrollment.PathStepPlan
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

class UnknownLearningPathException(pathId: UUID) : RuntimeException("Learning path $pathId does not exist")

/** Carries the realized step sequence onto the final step's EnrollmentEventMessage — see that message type's doc comment. */
data class PathCompletionContext(val pathId: UUID, val pathVersionId: UUID, val realizedStepCourseIds: List<UUID>)

/**
 * Ch.21 §2/§3: drives a learner through a Learning Path by creating one
 * ordinary step `Enrollment` at a time and reacting synchronously to its
 * completion — no separate state machine, no new Kafka round-trip. This
 * reuses 100% of Enrollment's existing lifecycle/event-sourcing/outbox
 * machinery; PathProgress itself stays a plain projection (see its doc
 * comment), never a second source of truth for "is this course done."
 *
 * `onEnrollmentCompleted` is called both from a real HTTP request
 * (EnrollmentController.complete()) AND from a Kafka consumer thread
 * (AssessmentEventListener, reacting to GradingPassed) — the latter has no
 * user token to relay to course-management. That's exactly why
 * PathProgress.stepPlan pre-resolves every step's content version once, at
 * path-enrollment time (see its doc comment): step advancement never needs
 * an outbound HTTP call, so it works identically from either trigger.
 */
@Component
class PathProgressService(
    private val pathProgressRepository: PathProgressRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val learningPathClient: LearningPathClient,
    private val courseManagementClient: CourseManagementClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun startPath(tenantId: TenantId, learnerId: String, pathId: UUID, orgUnitId: UUID?, bearerToken: String): Pair<PathProgress, Enrollment> {
        val version = learningPathClient.getCurrentVersion(pathId, bearerToken) ?: throw UnknownLearningPathException(pathId)
        val orderedSteps = version.steps.sortedBy { it.stepOrder }
        check(orderedSteps.isNotEmpty()) { "Learning path $pathId's current version has no steps" }

        val stepPlan = orderedSteps.map { step ->
            val courseVersion = courseManagementClient.getCurrentVersion(step.courseId.toString(), bearerToken)
                ?: error("Course ${step.courseId} (referenced by path $pathId) does not exist")
            PathStepPlan(step.courseId, courseVersion.versionId)
        }

        val now = Instant.now()
        val progress = PathProgress(
            pathProgressId = UUID.randomUUID(),
            tenantId = tenantId.value,
            learnerId = learnerId,
            pathId = pathId,
            pathVersionId = version.versionId,
            stepPlan = stepPlan,
            currentStepIndex = 0,
            status = PathProgressStatus.IN_PROGRESS,
            realizedStepCourseIds = emptyList(),
            createdAt = now,
            updatedAt = now,
        )
        pathProgressRepository.insert(progress)

        val enrollment = enrollForStep(tenantId, learnerId, stepPlan[0], orgUnitId, progress.pathProgressId)
        enrollment.start()
        enrollmentRepository.save(enrollment)
        return progress to enrollment
    }

    /**
     * Called right after a step's `Enrollment` transitions to COMPLETED in
     * memory but *before* that enrollment is saved — so, when this is the
     * path's last step, the returned context can be threaded into the very
     * same `repository.save()` call that publishes the completion event,
     * with no separate write and therefore no ordering race against it.
     * Purely in-process — see this class's doc comment for why no HTTP call
     * happens here.
     */
    fun onEnrollmentCompleted(enrollment: Enrollment): PathCompletionContext? {
        val pathProgressId = enrollment.pathProgressId ?: return null
        val progress = pathProgressRepository.findById(pathProgressId)
            ?: error("Enrollment ${enrollment.enrollmentId} references missing PathProgress $pathProgressId")

        val realized = progress.realizedStepCourseIds + UUID.fromString(enrollment.courseId)
        val nextIndex = progress.currentStepIndex + 1

        if (nextIndex >= progress.stepPlan.size) {
            pathProgressRepository.update(progress.copy(status = PathProgressStatus.COMPLETED, realizedStepCourseIds = realized, updatedAt = Instant.now()))
            log.info("Path {} completed for learner {} — realized steps: {}", progress.pathId, progress.learnerId, realized)
            return PathCompletionContext(progress.pathId, progress.pathVersionId, realized)
        }

        pathProgressRepository.update(progress.copy(currentStepIndex = nextIndex, realizedStepCourseIds = realized, updatedAt = Instant.now()))
        val nextStep = progress.stepPlan[nextIndex]
        val nextEnrollment = enrollForStep(enrollment.tenantId, progress.learnerId, nextStep, enrollment.orgUnitId, pathProgressId)
        nextEnrollment.start()
        enrollmentRepository.save(nextEnrollment)
        log.info("Path {} advanced to step {} ({}) for learner {}", progress.pathId, nextIndex, nextStep.courseId, progress.learnerId)
        return null
    }

    private fun enrollForStep(tenantId: TenantId, learnerId: String, step: PathStepPlan, orgUnitId: UUID?, pathProgressId: UUID): Enrollment =
        Enrollment.enroll(UUID.randomUUID(), tenantId, learnerId, step.courseId.toString(), step.contentVersionId, orgUnitId, pathProgressId)
}
