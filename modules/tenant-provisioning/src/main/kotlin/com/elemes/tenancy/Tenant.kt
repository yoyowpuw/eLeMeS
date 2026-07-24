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
 *
 * `MIGRATING` (Ch.12 §2/Ch.18 §6, added for the pool-to-silo migration the
 * AKB itself only ever defined the cost trigger for, never a mechanism —
 * see [[TenantController.migrateToSilo]]'s doc comment): an ACTIVE POOLED
 * tenant transitions here for the duration of its data being copied to a
 * new dedicated database. Reuses the exact same enforcement path as
 * OFFBOARDED — `authz.rego`'s `tenant_active` only allows `ACTIVE` through,
 * so a MIGRATING tenant's traffic is denied everywhere, immediately, the
 * instant this is pushed to OPA, with zero new Rego logic. Transitions back
 * to ACTIVE (isolationTier now SILO) on success, or back to ACTIVE
 * (isolationTier still POOLED, best-effort) if the migration fails partway.
 */
enum class TenantStatus { PROVISIONING, ACTIVE, MIGRATING, OFFBOARDED }

/** Ch.12 §2/Ch.18 §3: pooled is the default; a SILO tenant gets a genuinely dedicated database — see SiloProvisioner. */
enum class IsolationTier { POOLED, SILO }

data class Tenant(
    val tenantId: String,
    val name: String,
    val isolationTier: IsolationTier,
    val region: String,
    val status: TenantStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** Base JDBC URL (no schema) of this tenant's dedicated database, set by SiloProvisioner — always null for a POOLED tenant. */
    val siloDatabase: String? = null,
)
