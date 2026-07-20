package elemes.authz

import future.keywords.if
import future.keywords.in

# Ch.17 ADR-028: OPA-class policy-as-code, evaluated as a local sidecar per
# service in production. Locally, one shared OPA container is queried by all
# five services over HTTP — a deliberate local-dev simplification (same
# category as Redpanda-for-Kafka, Keycloak-for-CIAM), not the real topology.
#
# Two independent checks, both must pass:
#   1. tenant_ok  — the caller's own tenant must match the resource's tenant.
#      Skipped only when there is no resource yet (creating something new).
#   2. role_ok    — either the action is open to any authenticated caller,
#      or the caller holds one of the roles required for that action. For
#      Ch.19 org-scoped actions specifically, holding "admin" is enough on
#      its own (tenant-wide), but holding only "manager" additionally
#      requires the resource to sit inside a subtree the manager actually
#      manages (org_ok) — see Ch.19 §2's "Manager Maya" example.
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
}

# Ch.19: actions in this set additionally require org_ok when the caller's
# only qualifying role is "manager" — "admin" always stays tenant-wide.
# "org_unit_create" is deliberately absent: a new unit is always created
# unparented, so there's no target org unit yet to scope against — see
# OrgUnitController.create()'s doc comment.
org_scoped_actions := {"revoke_certificate", "create_course", "publish_course_version", "org_unit_reparent"}

allow if {
	tenant_ok
	role_ok
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
