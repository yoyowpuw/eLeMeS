package com.elemes.common

/**
 * Ch.12 §2: the current request/message's tenant, threaded through to
 * [TenantAwareDataSource] so PostgreSQL Row-Level Security has something to
 * filter on. Set once per unit of work — an HTTP request (see
 * TenantContextFilter) or a Kafka message (set explicitly by the listener,
 * since there is no HTTP request to extract it from) — and always cleared
 * afterward so a pooled thread never leaks one request's tenant into the
 * next.
 */
object TenantContext {
    private val current = ThreadLocal<String?>()

    /**
     * Every RLS policy in this codebase carries an `OR current_setting(...)
     * = '*'` clause specifically for this sentinel. There is exactly one
     * legitimate reason to use it: Ch.26 §6 requires certificate `/verify`
     * to work for any caller, with no token and therefore no tenant to
     * scope to — see `CertificateController.verify()`, the only call site.
     * Anywhere else, reaching for this is almost certainly a bug, not a
     * feature — it defeats the isolation this whole mechanism exists for.
     */
    const val BYPASS = "*"

    fun set(tenantId: String) {
        current.set(tenantId)
    }

    fun setBypass() {
        current.set(BYPASS)
    }

    fun get(): String? = current.get()

    fun clear() {
        current.remove()
    }
}
