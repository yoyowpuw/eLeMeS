package com.elemes.enrollment.infrastructure

import com.elemes.enrollment.PathProgress
import com.elemes.enrollment.PathProgressStatus
import com.elemes.enrollment.PathStepPlan
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/** Plain JDBC CRUD, not event-sourced — see PathProgress's doc comment for why. */
@Repository
class PathProgressRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {
    fun insert(progress: PathProgress) {
        jdbcTemplate.update(
            """
            insert into path_progress
                (path_progress_id, tenant_id, learner_id, path_id, path_version_id, step_plan, current_step_index, status, realized_step_course_ids, created_at, updated_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            progress.pathProgressId, progress.tenantId, progress.learnerId, progress.pathId, progress.pathVersionId,
            objectMapper.writeValueAsString(progress.stepPlan), progress.currentStepIndex, progress.status.name,
            toJson(progress.realizedStepCourseIds), Timestamp.from(progress.createdAt), Timestamp.from(progress.updatedAt),
        )
    }

    fun update(progress: PathProgress) {
        jdbcTemplate.update(
            "update path_progress set current_step_index = ?, status = ?, realized_step_course_ids = ?, updated_at = ? where path_progress_id = ?",
            progress.currentStepIndex, progress.status.name, toJson(progress.realizedStepCourseIds),
            Timestamp.from(progress.updatedAt), progress.pathProgressId,
        )
    }

    fun findById(pathProgressId: UUID): PathProgress? =
        jdbcTemplate.query(
            """
            select path_progress_id, tenant_id, learner_id, path_id, path_version_id, step_plan, current_step_index, status, realized_step_course_ids, created_at, updated_at
            from path_progress where path_progress_id = ?
            """.trimIndent(),
            { rs, _ ->
                PathProgress(
                    pathProgressId = UUID.fromString(rs.getString("path_progress_id")),
                    tenantId = rs.getString("tenant_id"),
                    learnerId = rs.getString("learner_id"),
                    pathId = UUID.fromString(rs.getString("path_id")),
                    pathVersionId = UUID.fromString(rs.getString("path_version_id")),
                    stepPlan = objectMapper.readValue(rs.getString("step_plan"), jacksonTypeRef<List<PathStepPlan>>()),
                    currentStepIndex = rs.getInt("current_step_index"),
                    status = PathProgressStatus.valueOf(rs.getString("status")),
                    realizedStepCourseIds = fromJson(rs.getString("realized_step_course_ids")),
                    createdAt = rs.getTimestamp("created_at").toInstant(),
                    updatedAt = rs.getTimestamp("updated_at").toInstant(),
                )
            },
            pathProgressId,
        ).firstOrNull()

    private fun toJson(courseIds: List<UUID>): String = objectMapper.writeValueAsString(courseIds.map { it.toString() })
    private fun fromJson(json: String): List<UUID> =
        objectMapper.readValue(json, Array<String>::class.java).map(UUID::fromString)
}
