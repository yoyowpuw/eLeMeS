package com.elemes.assessment

import com.elemes.common.EventSourcedAggregate
import com.elemes.common.TenantId
import java.util.UUID

enum class AssessmentStatus { STARTED, SUBMITTED, PASSED, FAILED }

class Assessment private constructor(
    val assessmentId: UUID,
) : EventSourcedAggregate<AssessmentEvent>() {

    private var _tenantId: TenantId? = null
    val tenantId: TenantId get() = _tenantId ?: error("Assessment $assessmentId has no events applied yet")

    lateinit var enrollmentId: UUID
        private set
    lateinit var courseId: String
        private set
    var passingScore: Int = 70
        private set
    private var questions: List<Question> = emptyList()
    var status: AssessmentStatus = AssessmentStatus.STARTED
        private set
    var score: Int? = null
        private set

    companion object {
        fun start(
            assessmentId: UUID,
            tenantId: TenantId,
            enrollmentId: UUID,
            courseId: String,
            questions: List<Question>,
            passingScore: Int = 70,
        ): Assessment {
            require(questions.isNotEmpty()) { "An assessment needs at least one question" }
            val assessment = Assessment(assessmentId)
            assessment.raise(AssessmentStarted(assessmentId, tenantId, enrollmentId, courseId, questions, passingScore))
            return assessment
        }

        fun rehydrate(assessmentId: UUID, history: List<AssessmentEvent>): Assessment {
            val assessment = Assessment(assessmentId)
            assessment.loadFromHistory(history)
            return assessment
        }
    }

    /** Ch.24 §2 deterministic auto-grading path; manual grading (Ch.23 §4) is not modeled here. */
    fun submit(answers: Map<String, Int>) {
        check(status == AssessmentStatus.STARTED) { "Cannot submit assessment in status $status" }
        raise(AssessmentSubmitted(assessmentId, tenantId, enrollmentId, answers))

        val correct = questions.count { answers[it.questionId] == it.correctOptionIndex }
        val computedScore = correct * 100 / questions.size
        raise(AssessmentGraded(assessmentId, tenantId, enrollmentId, computedScore))

        if (computedScore >= passingScore) {
            raise(AssessmentPassed(assessmentId, tenantId, enrollmentId, computedScore))
        } else {
            raise(AssessmentFailed(assessmentId, tenantId, enrollmentId, computedScore))
        }
    }

    override fun apply(event: AssessmentEvent) {
        when (event) {
            is AssessmentStarted -> {
                _tenantId = event.tenantId
                enrollmentId = event.enrollmentId
                courseId = event.courseId
                questions = event.questions
                passingScore = event.passingScore
                status = AssessmentStatus.STARTED
            }
            is AssessmentSubmitted -> status = AssessmentStatus.SUBMITTED
            is AssessmentGraded -> score = event.score
            is AssessmentPassed -> status = AssessmentStatus.PASSED
            is AssessmentFailed -> status = AssessmentStatus.FAILED
        }
    }
}
