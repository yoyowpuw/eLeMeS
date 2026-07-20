package com.elemes.course

import java.time.Instant
import java.util.UUID

/** Ch.21 §2/§3: a path's mutable pointer to its currently-published version — mirrors Course/ContentVersion exactly. */
data class LearningPath(
    val pathId: UUID,
    val tenantId: String,
    val name: String,
    val createdAt: Instant,
    val currentVersionId: UUID,
    /** Ch.19: opt-in, used only for manager-scoped authorization (create/publish) — same as Course.orgUnitId. */
    val orgUnitId: UUID? = null,
)

/** Ch.12 §7-style immutable, insert-only version — never updated in place. */
data class PathVersion(
    val versionId: UUID,
    val tenantId: String,
    val pathId: UUID,
    val versionNumber: Int,
    val createdAt: Instant,
)

/**
 * Ch.21 §2: v1 scope is strict-sequence ordering only (`stepOrder`, always
 * consumed in order) — unordered-set and conditional-branch modes are
 * explicitly deferred, not silently unsupported; see README's deferred list.
 */
data class PathStep(
    val stepId: UUID,
    val pathVersionId: UUID,
    val stepOrder: Int,
    val courseId: UUID,
)
