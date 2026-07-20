package com.elemes.course.infrastructure

import com.elemes.course.LearningPath
import com.elemes.course.PathStep
import com.elemes.course.PathVersion
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class UnknownCourseException(courseId: UUID) : RuntimeException("Course $courseId does not exist")

data class PathVersionWithSteps(val version: PathVersion, val steps: List<PathStep>)

/** Mirrors CourseRepository's create/publishNewVersion shape exactly — same insert-only versioning pattern, applied to path structure instead of content. */
@Repository
class LearningPathRepository(private val jdbcTemplate: JdbcTemplate) {

    /**
     * Same FK-ordering trick as Course/ContentVersion: insert the path with a
     * null current-version pointer, insert version 1 + its steps (their FKs
     * are now satisfiable), then point the path at it — one transaction.
     */
    @Transactional
    fun create(pathId: UUID, tenantId: String, name: String, courseIds: List<UUID>, orgUnitId: UUID? = null): LearningPath {
        requireKnownCourses(courseIds)
        val now = Instant.now()
        val versionId = UUID.randomUUID()

        jdbcTemplate.update(
            "insert into learning_paths (path_id, tenant_id, name, created_at, current_version_id, org_unit_id) values (?, ?, ?, ?, null, ?)",
            pathId, tenantId, name, Timestamp.from(now), orgUnitId,
        )
        insertVersion(PathVersion(versionId, tenantId, pathId, versionNumber = 1, createdAt = now), courseIds)
        jdbcTemplate.update("update learning_paths set current_version_id = ? where path_id = ?", versionId, pathId)

        return LearningPath(pathId, tenantId, name, now, versionId, orgUnitId)
    }

    /** Ch.21 §3 / ADR-034: publishing a new version never touches or invalidates prior versions — an in-flight PathProgress pinned to an old version stays valid. */
    @Transactional
    fun publishNewVersion(pathId: UUID, courseIds: List<UUID>): PathVersion? {
        requireKnownCourses(courseIds)
        val path = findById(pathId) ?: return null
        val currentNumber = findVersionById(path.currentVersionId)?.versionNumber ?: 0
        val version = PathVersion(UUID.randomUUID(), path.tenantId, pathId, currentNumber + 1, Instant.now())
        insertVersion(version, courseIds)
        jdbcTemplate.update("update learning_paths set current_version_id = ? where path_id = ?", version.versionId, pathId)
        return version
    }

    private fun requireKnownCourses(courseIds: List<UUID>) {
        require(courseIds.isNotEmpty()) { "A learning path needs at least one step" }
        courseIds.forEach { courseId ->
            val exists = jdbcTemplate.queryForObject(
                "select exists(select 1 from courses where course_id = ?)", Boolean::class.java, courseId,
            ) ?: false
            if (!exists) throw UnknownCourseException(courseId)
        }
    }

    private fun insertVersion(version: PathVersion, courseIds: List<UUID>) {
        jdbcTemplate.update(
            "insert into path_versions (version_id, tenant_id, path_id, version_number, created_at) values (?, ?, ?, ?, ?)",
            version.versionId, version.tenantId, version.pathId, version.versionNumber, Timestamp.from(version.createdAt),
        )
        courseIds.forEachIndexed { index, courseId ->
            jdbcTemplate.update(
                "insert into path_steps (step_id, tenant_id, path_version_id, step_order, course_id) values (?, ?, ?, ?, ?)",
                UUID.randomUUID(), version.tenantId, version.versionId, index, courseId,
            )
        }
    }

    fun findById(pathId: UUID): LearningPath? =
        jdbcTemplate.query(
            "select path_id, tenant_id, name, created_at, current_version_id, org_unit_id from learning_paths where path_id = ?",
            { rs, _ ->
                LearningPath(
                    pathId = UUID.fromString(rs.getString("path_id")),
                    tenantId = rs.getString("tenant_id"),
                    name = rs.getString("name"),
                    createdAt = rs.getTimestamp("created_at").toInstant(),
                    currentVersionId = UUID.fromString(rs.getString("current_version_id")),
                    orgUnitId = rs.getString("org_unit_id")?.let(UUID::fromString),
                )
            },
            pathId,
        ).firstOrNull()

    fun findVersionById(versionId: UUID): PathVersion? =
        jdbcTemplate.query(
            "select version_id, tenant_id, path_id, version_number, created_at from path_versions where version_id = ?",
            { rs, _ ->
                PathVersion(
                    versionId = UUID.fromString(rs.getString("version_id")),
                    tenantId = rs.getString("tenant_id"),
                    pathId = UUID.fromString(rs.getString("path_id")),
                    versionNumber = rs.getInt("version_number"),
                    createdAt = rs.getTimestamp("created_at").toInstant(),
                )
            },
            versionId,
        ).firstOrNull()

    fun findStepsByVersionId(versionId: UUID): List<PathStep> =
        jdbcTemplate.query(
            "select step_id, path_version_id, step_order, course_id from path_steps where path_version_id = ? order by step_order",
            { rs, _ ->
                PathStep(
                    stepId = UUID.fromString(rs.getString("step_id")),
                    pathVersionId = UUID.fromString(rs.getString("path_version_id")),
                    stepOrder = rs.getInt("step_order"),
                    courseId = UUID.fromString(rs.getString("course_id")),
                )
            },
            versionId,
        )

    fun findCurrentVersionWithSteps(pathId: UUID): PathVersionWithSteps? {
        val path = findById(pathId) ?: return null
        val version = findVersionById(path.currentVersionId) ?: return null
        return PathVersionWithSteps(version, findStepsByVersionId(version.versionId))
    }
}
