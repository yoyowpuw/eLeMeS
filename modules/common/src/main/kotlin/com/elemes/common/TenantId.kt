package com.elemes.common

/**
 * The one deliberately shared value object between bounded contexts (Ch.11 §4
 * Shared Kernel: Tenancy <-> Org Hierarchy) — every aggregate, event, and table
 * in this codebase carries a TenantId so pooled multi-tenancy (Ch.12 §2) can be
 * added without a schema change later, even while running single-tenant locally.
 */
@JvmInline
value class TenantId(val value: String) {
    override fun toString(): String = value
}
