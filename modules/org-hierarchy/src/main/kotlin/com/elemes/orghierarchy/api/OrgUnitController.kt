package com.elemes.orghierarchy.api

import com.elemes.common.AuthzInput
import com.elemes.common.ForbiddenException
import com.elemes.common.OpaAuthorizer
import com.elemes.common.roles
import com.elemes.common.tenantId
import com.elemes.orghierarchy.OrgUnit
import com.elemes.orghierarchy.infrastructure.OrgUnitRepository
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID

data class CreateOrgUnitRequest(val name: String, val unitType: String, val managerUserId: String? = null)
data class ReparentRequest(val newParentId: UUID?, val hierarchyType: String)
data class OrgUnitResponse(val orgUnitId: UUID, val tenantId: String, val name: String, val unitType: String, val managerUserId: String?)

class OrgUnitNotFoundException(id: UUID) : RuntimeException("Org unit $id not found")

private const val DEFAULT_HIERARCHY_TYPE = "reporting-line"

@RestController
@RequestMapping("/api/v1/org-units")
class OrgUnitController(
    private val repository: OrgUnitRepository,
    private val authorizer: OpaAuthorizer,
) {

    /**
     * Ch.17 ADR-028: restricted to admin/manager, same tier as course
     * creation — shaping the org is not a learner action. Deliberately NOT
     * org-scoped (unlike reparent below): a new unit is always created
     * unparented — there's no target org unit to check the caller's
     * authority against yet, since attaching it anywhere is a separate,
     * already-scoped `reparent` call.
     */
    @PostMapping
    fun create(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: CreateOrgUnitRequest): ResponseEntity<OrgUnitResponse> {
        authorizer.check(AuthzInput("org_unit_create", jwt.tenantId().value, jwt.roles()))
        val unit = repository.create(UUID.randomUUID(), jwt.tenantId().value, request.name, request.unitType, request.managerUserId)
        return ResponseEntity.status(HttpStatus.CREATED).body(unit.toResponse())
    }

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ResponseEntity<OrgUnitResponse> {
        val unit = loadOrThrow(id)
        authorizer.check(AuthzInput("read_org_unit", jwt.tenantId().value, jwt.roles(), unit.tenantId))
        return ResponseEntity.ok(unit.toResponse())
    }

    /**
     * Ch.19 ADR-031/FR-006: rewrites the moved subtree's closure rows for
     * `hierarchyType` only — a unit's parent under a different hierarchyType
     * (e.g. "cost-center") is untouched, which is what makes matrixed
     * reporting (FR-009) possible.
     *
     * Ch.19 org-scoping: a manager (not admin) may only reparent a unit that
     * sits inside their own managed subtree, and — if moving it under a new
     * parent — that new parent must also be inside their subtree. Without
     * the second check, a manager could "give away" part of the org into a
     * hierarchy they don't control. `resourceOrgUnit` here is each unit's
     * own id, not some separate owning-unit field — an org unit's "org
     * scope" for this check is itself.
     */
    @PostMapping("/{id}/reparent")
    fun reparent(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @RequestBody request: ReparentRequest,
    ): ResponseEntity<OrgUnitResponse> {
        val unit = loadOrThrow(id)
        val roles = jwt.roles()
        val callerOrgUnits = resolveCallerScope(roles, jwt, request.hierarchyType)
        authorizer.check(
            AuthzInput("org_unit_reparent", jwt.tenantId().value, roles, unit.tenantId, callerOrgUnits, unit.orgUnitId.toString())
        )
        if (request.newParentId != null) {
            val parent = loadOrThrow(request.newParentId)
            authorizer.check(
                AuthzInput("org_unit_reparent", jwt.tenantId().value, roles, parent.tenantId, callerOrgUnits, parent.orgUnitId.toString())
            )
        }
        repository.reparent(id, request.newParentId, request.hierarchyType, unit.tenantId)
        return ResponseEntity.ok(unit.toResponse())
    }

    @GetMapping("/{id}/descendants")
    fun descendants(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @RequestParam(defaultValue = DEFAULT_HIERARCHY_TYPE) hierarchyType: String,
    ): ResponseEntity<List<OrgUnitResponse>> {
        val unit = loadOrThrow(id)
        authorizer.check(AuthzInput("read_org_unit", jwt.tenantId().value, jwt.roles(), unit.tenantId))
        return ResponseEntity.ok(repository.descendants(id, hierarchyType).map { it.toResponse() })
    }

    @GetMapping("/{id}/ancestors")
    fun ancestors(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @RequestParam(defaultValue = DEFAULT_HIERARCHY_TYPE) hierarchyType: String,
    ): ResponseEntity<List<OrgUnitResponse>> {
        val unit = loadOrThrow(id)
        authorizer.check(AuthzInput("read_org_unit", jwt.tenantId().value, jwt.roles(), unit.tenantId))
        return ResponseEntity.ok(repository.ancestors(id, hierarchyType).map { it.toResponse() })
    }

    /**
     * Ch.19: the set of org units the caller has authority over — the units
     * they directly manage plus every descendant of those units (Ch.19 §2's
     * "all reports under Maya" example). No OPA check here: this is always
     * about the caller's own scope, so there's no other tenant's data or
     * role gate to enforce — any authenticated caller may ask "what's mine".
     * Consumed by other services (e.g. Certification's manager-scoped
     * revocation check) via token-relay, the same pattern used elsewhere for
     * synchronous cross-service reads.
     */
    @GetMapping("/my-scope")
    fun myScope(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = DEFAULT_HIERARCHY_TYPE) hierarchyType: String,
    ): ResponseEntity<List<UUID>> = ResponseEntity.ok(resolveScope(jwt, hierarchyType))

    private fun resolveScope(jwt: Jwt, hierarchyType: String): List<UUID> {
        val managerUserId = jwt.getClaimAsString("preferred_username") ?: return emptyList()
        val managed = repository.findManagedBy(managerUserId, jwt.tenantId().value)
        return managed.flatMap { repository.descendants(it.orgUnitId, hierarchyType) }
            .map { it.orgUnitId }
            .distinct()
    }

    /**
     * Only resolved for "manager" (not "admin") callers — admin always
     * stays tenant-wide. Unlike Certification/Course Management's client of
     * the same shape, this resolves in-process against the repository
     * rather than over HTTP — org-hierarchy already owns this data, so a
     * self-call would be pure overhead.
     */
    private fun resolveCallerScope(roles: List<String>, jwt: Jwt, hierarchyType: String): List<String> =
        if ("admin" !in roles && "manager" in roles) {
            resolveScope(jwt, hierarchyType).map { it.toString() }
        } else {
            emptyList()
        }

    private fun loadOrThrow(id: UUID): OrgUnit = repository.findById(id) ?: throw OrgUnitNotFoundException(id)
}

private fun OrgUnit.toResponse() = OrgUnitResponse(orgUnitId, tenantId, name, unitType, managerUserId)

@RestControllerAdvice
class OrgUnitExceptionHandler {
    @ExceptionHandler(OrgUnitNotFoundException::class)
    fun handleNotFound(ex: OrgUnitNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "not found")))

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(ex: ForbiddenException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to (ex.message ?: "forbidden")))
}
