package com.elemes.course

import java.time.Instant
import java.util.UUID

/** Ch.12 §7: an immutable, insert-only content revision — never updated in place. */
data class ContentVersion(
    val versionId: UUID,
    val tenantId: String,
    val courseId: UUID,
    val versionNumber: Int,
    val contentHash: String,
    val createdAt: Instant,
)
