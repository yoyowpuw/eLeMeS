package com.elemes.course.infrastructure

import com.elemes.course.Course
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.util.UUID

@Repository
class CourseRepository(private val jdbcTemplate: JdbcTemplate) {

    fun save(course: Course) {
        jdbcTemplate.update(
            """
            insert into courses (course_id, tenant_id, code, title, created_at)
            values (?, ?, ?, ?, ?)
            """.trimIndent(),
            course.courseId,
            course.tenantId,
            course.code,
            course.title,
            Timestamp.from(course.createdAt),
        )
    }

    fun findById(courseId: UUID): Course? =
        jdbcTemplate.query(
            "select course_id, tenant_id, code, title, created_at from courses where course_id = ?",
            { rs, _ ->
                Course(
                    courseId = UUID.fromString(rs.getString("course_id")),
                    tenantId = rs.getString("tenant_id"),
                    code = rs.getString("code"),
                    title = rs.getString("title"),
                    createdAt = rs.getTimestamp("created_at").toInstant(),
                )
            },
            courseId,
        ).firstOrNull()
}
