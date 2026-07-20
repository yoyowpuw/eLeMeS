package com.elemes.tenancy

import java.time.Instant

/**
 * Ch.18 §4's provisioning lifecycle, trimmed to the states this codebase
 * actually models: `PROVISIONING` (registered, not yet serving traffic) ->
 * `ACTIVE` (Ch.18 §4's "Activate tenant" step) -> `OFFBOARDED` (Ch.18 §5 /
 * Ch.5 Phase 10 — contract termination; control-plane access revoked
 * immediately, data plane retained for the regulatory retention period,
 * not deleted). No path back from OFFBOARDED — matches offboarding being a
 * terminal, contract-driven event, not a togglable flag.
 */
enum class TenantStatus { PROVISIONING, ACTIVE, OFFBOARDED }

/** Ch.12 §2/Ch.18 §3: pooled is the default; silo is metadata only here — no separate cluster actually gets provisioned locally, see README. */
enum class IsolationTier { POOLED, SILO }

data class Tenant(
    val tenantId: String,
    val name: String,
    val isolationTier: IsolationTier,
    val region: String,
    val status: TenantStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)
