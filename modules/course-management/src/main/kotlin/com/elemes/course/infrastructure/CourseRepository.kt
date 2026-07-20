package com.elemes.course.infrastructure

import com.elemes.course.ContentVersion
import com.elemes.course.Course
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class CourseRepository(private val jdbcTemplate: JdbcTemplate) {

    /**
     * A course never exists without a current version — created atomically
     * as version 1. `courses.current_version_id` and
     * `content_versions.course_id` reference each other, so neither row can
     * be inserted first with both FKs satisfied: insert the course with a
     * null pointer, insert the version (now its course_id FK is satisfiable),
     * then point the course at it — all within this one transaction.
     */
    @Transactional
    fun create(
        courseId: UUID,
        tenantId: String,
        code: String,
        title: String,
        initialContentHash: String,
        orgUnitId: UUID? = null,
    ): Course {
        val now = Instant.now()
        val versionId = UUID.randomUUID()

        jdbcTemplate.update(
            "insert into courses (course_id, tenant_id, code, title, created_at, current_version_id, org_unit_id) values (?, ?, ?, ?, ?, null, ?)",
            courseId, tenantId, code, title, Timestamp.from(now), orgUnitId,
        )
        insertVersion(ContentVersion(versionId, tenantId, courseId, versionNumber = 1, contentHash = initialContentHash, createdAt = now))
        jdbcTemplate.update("update courses set current_version_id = ? where course_id = ?", versionId, courseId)

        return Course(courseId, tenantId, code, title, now, versionId, orgUnitId)
    }

    /**
     * Ch.12 §7: publishes a new content version and moves the course's
     * current-version pointer. The old version row is never touched — an
     * enrollment/certificate pinned to it before this call remains valid.
     */
    @Transactional
    fun publishNewVersion(courseId: UUID, contentHash: String): ContentVersion? {
        val course = findById(courseId) ?: return null
        val currentNumber = findVersionById(course.currentVersionId)?.versionNumber ?: 0
        val version = ContentVersion(UUID.randomUUID(), course.tenantId, courseId, currentNumber + 1, contentHash, Instant.now())
        insertVersion(version)
        jdbcTemplate.update("update courses set current_version_id = ? where course_id = ?", version.versionId, courseId)
        return version
    }

    private fun insertVersion(version: ContentVersion) {
        jdbcTemplate.update(
            """
            insert into content_versions (version_id, tenant_id, course_id, version_number, content_hash, created_at)
            values (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            version.versionId, version.tenantId, version.courseId,
            version.versionNumber, version.contentHash, Timestamp.from(version.createdAt),
        )
    }

    fun findById(courseId: UUID): Course? =
        jdbcTemplate.query(
            "select course_id, tenant_id, code, title, created_at, current_version_id, org_unit_id from courses where course_id = ?",
            { rs, _ ->
                Course(
                    courseId = UUID.fromString(rs.getString("course_id")),
                    tenantId = rs.getString("tenant_id"),
                    code = rs.getString("code"),
                    title = rs.getString("title"),
                    createdAt = rs.getTimestamp("created_at").toInstant(),
                    currentVersionId = UUID.fromString(rs.getString("current_version_id")),
                    orgUnitId = rs.getString("org_unit_id")?.let(UUID::fromString),
                )
            },
            courseId,
        ).firstOrNull()

    /** Historical versions stay fetchable forever, even after a newer version supersedes them. */
    fun findVersionById(versionId: UUID): ContentVersion? =
        jdbcTemplate.query(
            "select version_id, tenant_id, course_id, version_number, content_hash, created_at from content_versions where version_id = ?",
            { rs, _ -> mapVersion(rs) },
            versionId,
        ).firstOrNull()

    fun findCurrentVersion(courseId: UUID): ContentVersion? =
        findById(courseId)?.let { findVersionById(it.currentVersionId) }

    private fun mapVersion(rs: ResultSet) = ContentVersion(
        versionId = UUID.fromString(rs.getString("version_id")),
        tenantId = rs.getString("tenant_id"),
        courseId = UUID.fromString(rs.getString("course_id")),
        versionNumber = rs.getInt("version_number"),
        contentHash = rs.getString("content_hash"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
    )
}
