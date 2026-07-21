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
	# Ch.21: same admin/manager + opt-in org-scoping shape as course
	# create/publish — a LearningPath is just ordered Course references.
	"create_learning_path": {"admin", "manager"},
	"publish_path_version": {"admin", "manager"},
	"revoke_certificate": {"admin", "manager"},
	"org_unit_create": {"admin", "manager"},
	"org_unit_reparent": {"admin", "manager"},
	# Ch.18: tenant lifecycle management is platform-admin only — a
	# tenant's own "admin" role must NOT be able to provision or offboard
	# OTHER tenants (or even its own — offboarding is a platform decision,
	# not a self-service one). "tenant_read" is the one exception: a
	# tenant's own admin may read their own tenant's registry record
	# (self-service status visibility, tenant_ok-scoped below), so it
	# lists both roles.
	"tenant_create": {"platform-admin"},
	"tenant_list": {"platform-admin"},
	"tenant_read": {"admin", "platform-admin"},
	"tenant_activate": {"platform-admin"},
	"tenant_offboard": {"platform-admin"},
	# Ch.40 §3: rotating the certificate-signing key is admin-only — not a
	# manager-delegable action, no org-scoping angle (it's not scoped to
	# any single learner/course/org unit at all).
	"rotate_signing_key": {"admin"},
	# Ch.12 §2 silo tier: only reachable via tenant-provisioning's own
	# SiloProvisioner, itself already gated on tenant_create being
	# platform-admin-only — this is the receiving service's own
	# independent re-check of that same requirement, not a second grant.
	"provision_tenant_silo": {"platform-admin"},
}

# Ch.19: actions in this set additionally require org_ok when the caller's
# only qualifying role is "manager" — "admin" always stays tenant-wide.
# "org_unit_create" is deliberately absent: a new unit is always created
# unparented, so there's no target org unit yet to scope against — see
# OrgUnitController.create()'s doc comment.
org_scoped_actions := {"revoke_certificate", "create_course", "publish_course_version", "org_unit_reparent", "create_learning_path", "publish_path_version"}

# Ch.18: tenant-management actions are deliberately exempt from tenant_active
# below. Two independent reasons stack here: (1) platform-admin's own
# caller_tenant ("platform") is never itself a registered business tenant,
# so this mostly wouldn't bite that role anyway — but (2) "tenant_read" is
# also held by plain "admin", and a tenant's own admin must still be able
# to see their OWN tenant's status after it's offboarded (to see *why*
# they're locked out) or while still PROVISIONING — exempting it here is
# what makes that possible without a separate carve-out.
tenant_management_actions := {"tenant_create", "tenant_list", "tenant_read", "tenant_activate", "tenant_offboard"}

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

# Ch.18: platform-admin is inherently cross-tenant — its own caller_tenant
# ("platform") will never equal any real business tenant's id, so without
# this a strict equality check would lock platform-admin out of reading
# every tenant's own record. role_ok (below) is still what actually gates
# which actions platform-admin may perform — this only means "there's no
# single tenant this role is confined to," not "this role can do anything."
tenant_ok if {
	"platform-admin" in input.caller_roles
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
	"platform-admin" in required
	"platform-admin" in input.caller_roles
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
