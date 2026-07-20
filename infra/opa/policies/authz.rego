package elemes.authz

import future.keywords.if
import future.keywords.in

# Ch.17 ADR-028: OPA-class policy-as-code, evaluated as a local sidecar per
# service in production. Locally, one shared OPA container is queried by all
# four services over HTTP — a deliberate local-dev simplification (same
# category as Redpanda-for-Kafka, Keycloak-for-CIAM), not the real topology.
#
# Two independent checks, both must pass:
#   1. tenant_ok  — the caller's own tenant must match the resource's tenant.
#      Skipped only when there is no resource yet (creating something new).
#   2. role_ok    — either the action is open to any authenticated caller,
#      or the caller holds one of the roles required for that action.
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
	"revoke_certificate": {"admin"},
	"org_unit_create": {"admin", "manager"},
	"org_unit_reparent": {"admin", "manager"},
}

allow if {
	tenant_ok
	role_ok
}


# Rego gotcha worth documenting: `not input.resource_tenant` only matches
# when the field is genuinely UNDEFINED (absent), not when it's present as
# JSON `null` — and Jackson serializes a Kotlin `null` as an explicit JSON
# null, not an omitted field. Both cases are checked explicitly so this
# doesn't silently depend on Kotlin-side serialization behavior.
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
	some role in input.caller_roles
	role in required
}
