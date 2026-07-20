package com.elemes.course

import java.time.Instant
import java.util.UUID

data class Course(
    val courseId: UUID,
    val tenantId: String,
    val code: String,
    val title: String,
    val createdAt: Instant,
    /** Always set — creating a course always creates version 1 in the same transaction (Ch.12 §7). */
    val currentVersionId: UUID,
)
