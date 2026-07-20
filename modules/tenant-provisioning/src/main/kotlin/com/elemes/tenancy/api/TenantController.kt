package com.elemes.tenancy.api

import com.elemes.common.AuthzInput
import com.elemes.common.ForbiddenException
import com.elemes.common.OpaAuthorizer
import com.elemes.common.roles
import com.elemes.common.tenantId
import com.elemes.tenancy.IsolationTier
import com.elemes.tenancy.Tenant
import com.elemes.tenancy.TenantStatus
import com.elemes.tenancy.infrastructure.OpaDataPusher
import com.elemes.tenancy.infrastructure.TenantRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice

data class CreateTenantRequest(val tenantId: String, val name: String, val isolationTier: IsolationTier = IsolationTier.POOLED, val region: String)
data class TenantResponse(val tenantId: String, val name: String, val isolationTier: String, val region: String, val status: String)

class TenantNotFoundException(id: String) : RuntimeException("Tenant $id not found")
class InvalidTenantTransitionException(id: String, from: TenantStatus, to: TenantStatus) :
    RuntimeException("Cannot transition tenant $id from $from to $to")

/**
 * Ch.18 §2/§4: the control plane's own admin surface. Lifecycle management
 * (create/activate/offboard/list) requires the "platform-admin" role — a
 * separate Keycloak realm role, held by a dedicated `platform-ops` user
 * with no real business tenant, distinct from any tenant's own "admin"
 * role. A tenant's own admin can only read their own tenant's record
 * (`get()`), not manage any tenant's lifecycle, including their own —
 * offboarding is a platform decision, not self-service.
 */
@RestController
@RequestMapping("/api/v1/tenants")
class TenantController(
    private val repository: TenantRepository,
    private val authorizer: OpaAuthorizer,
    private val opaDataPusher: OpaDataPusher,
) {

    @PostMapping
    fun create(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: CreateTenantRequest): ResponseEntity<TenantResponse> {
        authorizer.check(AuthzInput("tenant_create", jwt.tenantId().value, jwt.roles()))
        val tenant = repository.create(request.tenantId, request.name, request.isolationTier, request.region)
        opaDataPusher.push(tenant)
        return ResponseEntity.status(HttpStatus.CREATED).body(tenant.toResponse())
    }

    /** A tenant's own "admin" may read their own record (e.g. to see why they're locked out post-offboard) — resourceTenant scoping via tenant_ok is what prevents reading anyone else's. platform-admin can read any. */
    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: String): ResponseEntity<TenantResponse> {
        val tenant = loadOrThrow(id)
        authorizer.check(AuthzInput("tenant_read", jwt.tenantId().value, jwt.roles(), tenant.tenantId))
        return ResponseEntity.ok(tenant.toResponse())
    }

    /** Platform-admin only — the whole registry, not scoped to any single tenant. */
    @GetMapping
    fun list(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<List<TenantResponse>> {
        authorizer.check(AuthzInput("tenant_list", jwt.tenantId().value, jwt.roles()))
        return ResponseEntity.ok(repository.findAll().map { it.toResponse() })
    }

    /** Ch.18 §4's "Activate tenant (learner traffic enabled)" step — the SSO/SCIM/HRIS configuration in between is not modeled, this just flips the status. */
    @PostMapping("/{id}/activate")
    fun activate(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: String): ResponseEntity<TenantResponse> {
        authorizer.check(AuthzInput("tenant_activate", jwt.tenantId().value, jwt.roles()))
        val tenant = loadOrThrow(id)
        if (tenant.status != TenantStatus.PROVISIONING) throw InvalidTenantTransitionException(id, tenant.status, TenantStatus.ACTIVE)
        val updated = repository.updateStatus(id, TenantStatus.ACTIVE) ?: throw TenantNotFoundException(id)
        opaDataPusher.push(updated)
        return ResponseEntity.ok(updated.toResponse())
    }

    /**
     * Ch.18 §5 / Ch.5 Phase 10: control-plane access is revoked the moment
     * this returns — OpaDataPusher updates OPA's in-memory copy of this
     * tenant's status synchronously, before the response goes out, and
     * every other service's `allow` check consults it on every request.
     * Data plane (the actual business data in every other service) is
     * deliberately untouched here — retained for the regulatory retention
     * period, not deleted, per Ch.5 Phase 10.
     */
    @PostMapping("/{id}/offboard")
    fun offboard(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: String): ResponseEntity<TenantResponse> {
        authorizer.check(AuthzInput("tenant_offboard", jwt.tenantId().value, jwt.roles()))
        val tenant = loadOrThrow(id)
        if (tenant.status == TenantStatus.OFFBOARDED) throw InvalidTenantTransitionException(id, tenant.status, TenantStatus.OFFBOARDED)
        val updated = repository.updateStatus(id, TenantStatus.OFFBOARDED) ?: throw TenantNotFoundException(id)
        opaDataPusher.push(updated)
        return ResponseEntity.ok(updated.toResponse())
    }

    private fun loadOrThrow(id: String): Tenant = repository.findById(id) ?: throw TenantNotFoundException(id)
}

private fun Tenant.toResponse() = TenantResponse(tenantId, name, isolationTier.name, region, status.name)

@RestControllerAdvice
class TenantExceptionHandler {
    @ExceptionHandler(TenantNotFoundException::class)
    fun handleNotFound(ex: TenantNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "not found")))

    @ExceptionHandler(InvalidTenantTransitionException::class)
    fun handleInvalidTransition(ex: InvalidTenantTransitionException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to (ex.message ?: "invalid transition")))

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(ex: ForbiddenException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to (ex.message ?: "forbidden")))
}
