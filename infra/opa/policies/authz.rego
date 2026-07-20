package elemes.authz

import future.keywords.if
import future.keywords.in

# Ch.17 ADR-028: OPA-class policy-as-code, evaluated as a local sidecar per
# service in production. Locally, one shared OPA container is queried by all
# six services over HTTP — a deliberate local-dev simplification (same
# category as Redpanda-for-Kafka, Keycloak-for-CIAM), not the real topology.
#
# Three independent checks, all must pass:
#   1. tenant_ok    — the caller's own tenant must match the resource's
#      tenant. Skipped only when there is no resource yet (creating
#      something new).
#   2. role_ok      — either the action is open to any authenticated caller,
#      or the caller holds one of the roles required for that action. For
#      Ch.19 org-scoped actions specifically, holding "admin" is enough on
#      its own (tenant-wide), but holding only "manager" additionally
#      requires the resource to sit inside a subtree the manager actually
#      manages (org_ok) — see Ch.19 §2's "Manager Maya" example.
#   3. tenant_active — Ch.18 §5: an offboarded tenant's callers are denied
#      everywhere, immediately, regardless of role or resource — checked
#      against `data.tenants`, pushed here directly by tenant-provisioning
#      on every status transition (see OpaDataPusher's doc comment for why
#      push, not a synchronous call from here).
# Default is deny — an action absent from both action tables is rejected,
# not silently allowed.

default allow := false

open_actions := {
	"read_course",
	"create_enrollment",
	"read_enrollment",
	"create_assessment",
	"submit_assessment",
	"read_assessment",
	"read_certificate",
	"read_org_unit",
}

restricted_actions := {
	"create_course": {"admin", "manager"},
	"publish_course_version": {"admin", "manager"},
	"revoke_certificate": {"admin", "manager"},
	"org_unit_create": {"admin", "manager"},
	"org_unit_reparent": {"admin", "manager"},
	"tenant_create": {"admin"},
	"tenant_read": {"admin"},
	"tenant_activate": {"admin"},
	"tenant_offboard": {"admin"},
}

# Ch.19: actions in this set additionally require org_ok when the caller's
# only qualifying role is "manager" — "admin" always stays tenant-wide.
# "org_unit_create" is deliberately absent: a new unit is always created
# unparented, so there's no target org unit yet to scope against — see
# OrgUnitController.create()'s doc comment.
org_scoped_actions := {"revoke_certificate", "create_course", "publish_course_version", "org_unit_reparent"}

# Ch.18: tenant-management actions are deliberately exempt from tenant_active
# below — otherwise a tenant stuck in PROVISIONING could never call
# tenant_activate on itself (tenant_active would have to already be true for
# an action whose entire purpose is making it true), and an OFFBOARDED
# tenant's admin could never be re-onboarded. A real system would perform
# these as a separate platform-ops identity, entirely outside any single
# tenant's own active/offboarded status; this codebase has no such identity
# space, so the exemption is scoped to exactly these four actions instead.
tenant_management_actions := {"tenant_create", "tenant_read", "tenant_activate", "tenant_offboard"}

allow if {
	tenant_ok
	role_ok
	tenant_active
}

allow if {
	tenant_ok
	role_ok
	input.action in tenant_management_actions
}

# Unknown-tenant-allowed, not unknown-tenant-denied: a caller_tenant absent
# from data.tenants hasn't been registered in the control plane at all yet
# (true of every seeded demo tenant before tenant-provisioning existed) —
# treating that as "not yet migrated" rather than "blocked" avoids a hard
# cutover. A tenant IS present, it must be ACTIVE — PROVISIONING and
# OFFBOARDED both deny, which is the entire point for OFFBOARDED and an
# acceptable side effect for PROVISIONING (not yet meant to serve traffic).
tenant_active if {
	not data.tenants[input.caller_tenant]
}

tenant_active if {
	data.tenants[input.caller_tenant].status == "ACTIVE"
}


# Rego gotcha worth documenting: `not input.resource_tenant` only matches
# when the field is genuinely UNDEFINED (absent), not when it's present as
# JSON `null` — and Jackson serializes a Kotlin `null` as an explicit JSON
# null, not an omitted field. Both cases are checked explicitly so this
# doesn't silently depend on Kotlin-side serialization behavior. The same
# pattern is repeated below for resource_org_unit.
tenant_ok if {
	not input.resource_tenant
}

tenant_ok if {
	input.resource_tenant == null
}

tenant_ok if {
	input.resource_tenant == input.caller_tenant
}

role_ok if {
	input.action in open_actions
}

role_ok if {
	required := restricted_actions[input.action]
	"admin" in required
	"admin" in input.caller_roles
}

role_ok if {
	required := restricted_actions[input.action]
	"manager" in required
	"manager" in input.caller_roles
	not org_scoped_actions[input.action]
}

role_ok if {
	required := restricted_actions[input.action]
	"manager" in required
	"manager" in input.caller_roles
	org_scoped_actions[input.action]
	org_ok
}

# A resource with no org unit assigned (never set, or the feature predates
# it) is treated as accessible — org-scoping only restricts resources that
# actually opted into having an owning org unit.
org_ok if {
	not input.resource_org_unit
}

org_ok if {
	input.resource_org_unit == null
}

org_ok if {
	input.resource_org_unit in input.caller_org_units
}
