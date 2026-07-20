# elemes — Enterprise LMS

Implementation scaffold for the Architecture Knowledge Base at [`docs/akb/`](docs/akb/00-index.md).
Start there for the full architecture reasoning — this README covers only how to run
what exists today.

## What this is right now

**The full compliance-critical-tier golden path (Ch.11 §5) is closed, end to
end, with a real cryptographic proof at the finish line.** Five independent
Spring Boot services:

- **course-management** (`:8083`) — plain CRUD (Ch.10 §3: Supporting tier, no
  event sourcing), plus Ch.12 §7 content versioning: a course's content is
  hash-addressed and insert-only — publishing a new version moves a
  "current version" pointer without ever touching prior version rows. Called
  synchronously by Enrollment both to validate a `courseId` exists and to
  fetch the version that's current *right now* (Ch.11 §3 Customer-Supplier
  relationship).
- **assignment-enrollment** (`:8081`) — event-sourced Enrollment aggregate
  (Ch.12 §5), full Ch.5 §4 state machine: `Assigned → InProgress →
  (AwaitingGrading ⇄ InProgress) → Completed`. Pins the course's content
  version at `LearnerEnrolled` time (Ch.5 ADR-005, Ch.21 §7) and never
  re-queries it. Publishes every committed event to Kafka.
- **assessment** (`:8082`) — event-sourced Assessment aggregate (Ch.11 #9),
  auto-grades multiple-choice submissions, publishes `AssessmentSubmitted/
  Passed/Failed` onto Kafka.
- **certification** (`:8084`) — event-sourced Certificate aggregate (Ch.11
  #10, Restricted-Evidentiary per Ch.40 §2). Consumes Enrollment's
  `ContentCompleted`/`GradingPassed` events, issues a PKI-signed certificate
  (Ch.26 ADR-043) that pins the exact content version the learner enrolled
  against — not whatever's current when the certificate is issued — exposes
  independent verification, and supports append-only revocation (Ch.41 §3).
- **org-hierarchy** (`:8085`) — plain CRUD org-unit service (Ch.10 §3:
  Standard tier, no event sourcing) implementing Ch.19's PostgreSQL
  closure-table hierarchy model: each `org_units` row can sit at any position
  in any number of independent hierarchy types (`reporting-line`,
  `cost-center`, ...) simultaneously, and re-parenting a subtree rewrites
  only the affected closure rows in one transaction, never a migration
  project.

**What's proven, concretely — not asserted, actually run and checked:**
1. Enrolling against a real course; an unknown `courseId` is rejected with a
   real 400 from a real cross-service HTTP call.
2. Passing an assessment drives Enrollment `AwaitingGrading → Completed` via
   a real Kafka consumer; failing it drives the remediation loop back to
   `InProgress` — both confirmed against the raw event log, not just the API
   response.
3. Enrollment completion drives Certification to issue a signed certificate,
   two Kafka hops downstream (Assessment → Enrollment → Certification),
   confirmed by polling until it appears.
4. **The signature is real**, not decorative: directly UPDATE-ing the `score`
   field inside the certificate's own immutable event-log row in Postgres —
   bypassing the application entirely — flips `/verify` from `valid:true` to
   `valid:false`. Restoring the original value flips it back. This is Chapter
   26 ADR-043's entire reason for existing (checksums prove integrity;
   signatures prove *authenticity*), demonstrated, not just implemented.
5. Revocation is append-only: the original `CertificateIssued` event is never
   edited — a `CertificateRevoked` event is appended, and re-revoking an
   already-revoked certificate correctly 409s.
6. **A real broker outage doesn't lose data.** Redpanda was stopped, an
   assessment was submitted and passed (Assessment's own DB state committed
   fine — it doesn't need Kafka to grade), and the resulting event sat
   durably queued in Assessment's `outbox` table with `published_at IS NULL`.
   Enrollment correctly stayed `IN_PROGRESS` (it never received anything).
   Restarting Redpanda — no manual replay, no intervention — let the pollers
   catch up automatically: Enrollment flipped to `COMPLETED`, the certificate
   was issued moments later, and zero outbox rows were left stuck unpublished
   anywhere. This is the transactional outbox pattern (see below) proven
   under an actual failure, not just implemented.
7. **The certificate pins the version that existed at enrollment time, not
   whatever's current when it's issued.** A course was created (v1), a
   learner enrolled (pinning v1), the course was then *republished* to v2
   (simulating content being edited mid-enrollment), and only after that was
   the assessment completed. The resulting certificate's `contentVersionId`
   matched v1 — confirmed by comparing it directly against both version IDs,
   not just trusting the field name — while v1 itself remained independently
   fetchable from Course Management even though it was no longer "current."
   This is Chapter 5 ADR-005 / Chapter 21 §7's entire point, demonstrated
   against an actual version change, not a single-version happy path that
   could never have revealed whether pinning was real.
8. **Every request across all four services now requires a real token from
   Keycloak** (Ch.16 ADR-026's local stand-in for a bought CIAM platform) —
   an unauthenticated `POST /courses` gets a genuine 401, not a hypothetical
   one. `tenant_id` is read from the token's claims, not hardcoded — the
   whole golden path (course → enrollment → assessment → certificate) was
   re-run with a real token for `acme-corp`, and the tenant shows up
   correctly at every hop, including *after* two Kafka round-trips into
   Certification. Enrollment relays the caller's own token to Course
   Management's synchronous call (token-relay pattern) rather than using
   separate service credentials. `/certificates/{id}/verify` and
   `/certificates/public-key` deliberately stayed open with no token
   required — gating them would have directly contradicted Chapter 26 §6's
   requirement that a certificate be verifiable by a third party *without*
   platform access.
9. **Authorization is real too now — both role checks and cross-tenant
   denial**, enforced by every service querying a shared OPA policy (Ch.17
   ADR-028), not just authentication. Proven with seven distinct HTTP-level
   cases, not just direct policy queries: a `learner` token gets a genuine
   403 trying to create a course; an `admin` token succeeds; a token from
   `globex-corp` gets 403 reading a course that belongs to `acme-corp`; the
   same course is readable by an `acme-corp` token; the full golden path
   (enroll → assessment → certificate) still works end to end with
   authorization active; a `learner` gets 403 trying to revoke a
   certificate while `admin` succeeds; and `/verify` still needs no token
   at all throughout. One real bug surfaced and got fixed along the way:
   Rego's `not` only treats a field as falsy when it's genuinely *absent*,
   not when it's JSON `null` — and Jackson serializes Kotlin `null` as an
   explicit `null`, not an omitted field, so the original "no resource yet"
   policy branch silently never matched until this was corrected.
10. **Re-parenting a subtree actually rewrites only that subtree, in one
    transaction, and matrixed (dual-hierarchy) org units really are
    independent** (Ch.19 ADR-031) — not just claimed by the data model
    choice. Built a 3-level `reporting-line` tree (A → B → C), then
    re-parented B (carrying C with it) onto an unrelated root D, simulating
    a reorg per BR-006: querying A's descendants afterward returns only A;
    querying D's descendants returns D, B, C — the whole subtree moved
    atomically, with no migration step and no stale rows left connecting A
    to B or C. Separately, B was then also assigned a parent under a
    *different* hierarchy type (`cost-center`, to unit E) without touching
    its `reporting-line` parent at all — querying B's ancestors under each
    hierarchy type returns a different, independent chain, which is exactly
    what FR-009's matrixed/dual-manager reporting requires and the ADR-031
    technology evaluation claimed a closure table (not adjacency list or
    nested set) would deliver. Role/tenant checks on org-unit creation and
    re-parenting were verified the same way as the other services: a
    `learner` token gets 403, `admin` succeeds, a `globex-corp` token gets
    403 reading an `acme-corp` org unit.

## Tech stack (per the AKB's ADRs)

| Layer | Choice | Local dev stand-in |
|---|---|---|
| Language/runtime | Kotlin/JVM, JDK 21 LTS (Ch.15 ADR-023) | — |
| Framework | Spring Boot 3.3 | — (not pinned by the AKB; chosen for this scaffold) |
| Database | PostgreSQL (Ch.12 ADR-016) | Docker, `postgres:16-alpine`, **one schema per service** (`enrollment`, `assessment`, `course_mgmt`, `certification`, `org_hierarchy`) — genuine per-context data ownership on one shared local instance |
| Org hierarchy model | Closure table, PostgreSQL-native (Ch.19 ADR-031) | Implemented directly — no local stand-in needed, the AKB's selected technology *is* what's running locally |
| Event bus/store | Managed Kafka (Ch.15 ADR-024) | **Redpanda** — fully wired: assessment → enrollment → certification, published via a transactional outbox in each producer |
| Key management | Cloud HSM/KMS (Ch.40 ADR-066) | **Local RSA keypair**, file-persisted under `.local-kms/` (gitignored) — see deferred items |
| Object storage | S3-compatible (Ch.28 ADR-045) | MinIO in docker-compose — provisioned, still unused |
| Identity/Auth | Bought CIAM platform (Ch.16 ADR-026) | **Keycloak** — real OIDC, `tenant_id` custom claim, wired into all four services as OAuth2 Resource Servers |
| Authorization | Policy-as-code, OPA-class (Ch.17 ADR-028) | **Open Policy Agent** — one shared container locally (per-service sidecar in production), queried over HTTP by all four services; Rego policy in `infra/opa/policies/authz.rego` |

## Deliberately deferred (read this before assuming a gap is a bug)

- **Authentication and authorization are both real now.** Every endpoint
  requires a valid Keycloak token, `tenant_id` is derived from it, and every
  service checks the caller's tenant and role against a shared OPA policy
  (Ch.17 ADR-028) before acting — see "What's proven" #9. What's still
  missing is *granularity*: today's Rego policy is a flat tenant-match plus
  a handful of hardcoded role-to-action rules, not a real policy management
  surface, and there's no per-resource ownership check finer than "same
  tenant" (e.g. a `manager` can act on any resource in their tenant, not
  just ones they were assigned).
- **Org Hierarchy exists but nothing else consumes it yet.** The
  closure-table model, re-parenting, and matrixed hierarchy types are real
  and tested (see "What's proven" #10), but two things named in Ch.19 are
  deliberately not built: (1) `OrgUnitChanged`/`OrgUnitReparented` events
  aren't published to Kafka yet (§4) — there's no consumer for them yet,
  since Assignment's eligibility-computation use case (§3, ADR-032's 5-min
  TTL cache/fallback) doesn't exist as separate logic in this codebase; (2)
  no other service's OPA checks actually use org-unit membership yet — a
  `manager` can still act on any resource in their own tenant, not just
  ones within the org units they manage. Org Hierarchy is a real,
  independently-correct building block; wiring it into the other four
  services' authorization is the next step, not this one.
- **Certificate-signing key is a local file, not an HSM/KMS.** `.local-kms/`
  holds a plaintext RSA private key. It is persisted across restarts purely
  so local demo certificates keep verifying — this has zero of the protection
  Ch.40 §3 requires (HSM-backed, rotated, access-audited). Never let this
  file or its certificates be mistaken for anything but a local dev artifact.
- **Kafka consumption idempotency relies on guards, not dedup tracking.**
  Enrollment's `AssessmentEventListener` treats a duplicate delivery as a
  harmless no-op via `IllegalStateException` catch. Certification's guard is
  slightly stronger — an actual `enrollment_id UNIQUE` DB constraint — but
  still no explicit processed-message log.
- **Question Bank (Ch.24) is inlined into Assessment**, not its own context.
- **Learning Paths (Ch.21) don't exist as a concept** — enrollment is always
  against a single flat course. The certificate's realized branch/step
  sequence (also Ch.21 §7) therefore isn't modeled either; content-*version*
  pinning is (see "What's proven" #7), but there's no multi-step path to
  record a realized sequence *through* yet.
- **No idempotency-key handling** (Ch.13 §4) on mutation endpoints yet.
- **Single tenant, pooled-only.** Chapter 12 §2's hybrid silo/pool model and
  Chapter 18's control plane don't exist yet.
- **Testcontainers integration test is broken on this machine** — see below.

**Operational gotcha worth knowing about:** if you experiment with schema
changes via an IDE assistant (this repo has been used with GitHub Copilot in
parallel with direct work) and then revert the *source* changes without also
resetting the *database*, Flyway will refuse to boot with a checksum
mismatch — it applied a migration whose content no longer matches what's on
disk. Fix is `DROP SCHEMA <affected> CASCADE;` against the local Postgres
container (or `docker compose down -v` to nuke everything) and let Flyway
recreate it from the current source. This happened once already during
content-version-pinning work — resolved by dropping just the `course_mgmt`
and `certification` schemas without touching `enrollment`/`assessment`.

**Another gotcha, this time in Keycloak:** if you add users to
`infra/keycloak/realm-export.json` without `firstName`, `lastName`, and
`email`, login fails with a cryptic `"Account is not fully set up"` /
`resolve_required_actions` error — Keycloak 25's default User Profile
feature silently requires those fields to consider a profile complete, even
though nothing in the realm config says so explicitly. All three seeded
users already have them; keep that pattern for any new ones. If Keycloak's
realm ever needs re-importing after an export-file change, the container
must be recreated (`docker compose rm -sf keycloak && docker compose up -d
keycloak`) — `--import-realm` uses an IGNORE_EXISTING strategy and won't
re-apply changes to an already-imported realm on a simple restart.

**A Rego gotcha worth knowing about too:** `not input.some_field` in Rego
only succeeds when the field is genuinely *undefined* (the key is absent
from the JSON), not when it's present with value `null`. Jackson serializes
a Kotlin `null` as an explicit JSON `null`, not an omitted key, so a policy
rule written as `tenant_ok if { not input.resource_tenant }` silently never
matched for "no resource yet" requests coming from a Kotlin service — it had
to be paired with an explicit `input.resource_tenant == null` rule. Caught
by testing the policy directly against OPA's HTTP API with `curl` before
suspecting the Kotlin side at all; worth doing that first if a policy seems
to be denying something it shouldn't.

## Running locally

Prerequisites: Docker Desktop (WSL2 backend), JDK 21 (services target it via
Gradle toolchain regardless of your system `JAVA_HOME`).

```bash
# 1. Start infra (Keycloak's realm/client/users auto-import at startup —
#    give it a few seconds before requesting a token)
docker compose up -d postgres redpanda keycloak opa

# 2. Run all five services (each in its own terminal, or backgrounded)
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:course-management:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:assessment:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:assignment-enrollment:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:certification:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:org-hierarchy:bootRun
```

```bash
# 3. Get a real token (see infra/keycloak/realm-export.json for the seeded
#    users — learner1/admin1 in tenant "acme-corp", plus a second-tenant user)
TOKEN=$(curl -s -X POST http://localhost:8080/realms/elemes/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=elemes-service" \
  -d "username=learner1" -d "password=learner1" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

# 4. Course -> Enrollment -> Assessment -> Certificate, end to end, authenticated
COURSE_ID=$(curl -s -X POST localhost:8083/api/v1/courses \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"code":"SEC-101","title":"Security Awareness","initialContentHash":"sha256-placeholder-v1"}' \
  | grep -o '"courseId":"[^"]*"' | cut -d'"' -f4)

ENR_ID=$(curl -s -X POST localhost:8081/api/v1/enrollments \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d "{\"learnerId\":\"learner1\",\"courseId\":\"$COURSE_ID\"}" \
  | grep -o '"enrollmentId":"[^"]*"' | cut -d'"' -f4)
curl -X POST localhost:8081/api/v1/enrollments/$ENR_ID/start -H "Authorization: Bearer $TOKEN"
# Response's tenantId comes from the token's tenant_id claim ("acme-corp"),
# and contentVersionId is the version pinned for this enrollment.

ASM_ID=$(curl -s -X POST localhost:8082/api/v1/assessments -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "
{\"enrollmentId\":\"$ENR_ID\",\"courseId\":\"$COURSE_ID\",\"passingScore\":70,
 \"questions\":[{\"questionId\":\"q1\",\"text\":\"2+2?\",\"options\":[\"3\",\"4\"],\"correctOptionIndex\":1}]}" \
  | grep -o '"assessmentId":"[^"]*"' | cut -d'"' -f4)
curl -X POST localhost:8082/api/v1/assessments/$ASM_ID/submit \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{"answers":{"q1":1}}'

# Two Kafka hops later (Assessment -> Enrollment -> Certification):
curl localhost:8084/api/v1/certificates/by-enrollment/$ENR_ID -H "Authorization: Bearer $TOKEN"
# -> grab the certificateId, then verify WITHOUT a token — deliberately public per Ch.26 §6:
curl localhost:8084/api/v1/certificates/{certificateId}/verify   # {"valid":true}

# 5. OPA denies what it should. learner1 can't create a course (403):
curl -i -X POST localhost:8083/api/v1/courses \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"code":"SEC-102","title":"Nope","initialContentHash":"sha256-placeholder"}'
# -> 403 Forbidden. Get an admin1 token instead (same password grant, username=admin1)
# and it succeeds. A token for learner-other-tenant (tenant "globex-corp",
# password learner1 — note: NOT "learner-other-tenant", that's just the
# username) gets 403 reading $COURSE_ID above, since it belongs to "acme-corp".

# 6. Org Hierarchy: build a tree, then re-parent a subtree and watch it move
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/realms/elemes/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=elemes-service" -d "username=admin1" -d "password=admin1" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

A=$(curl -s -X POST localhost:8085/api/v1/org-units -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" -d '{"name":"Engineering","unitType":"division"}' | grep -o '"orgUnitId":"[^"]*"' | cut -d'"' -f4)
D=$(curl -s -X POST localhost:8085/api/v1/org-units -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" -d '{"name":"Operations","unitType":"division"}' | grep -o '"orgUnitId":"[^"]*"' | cut -d'"' -f4)
B=$(curl -s -X POST localhost:8085/api/v1/org-units -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" -d '{"name":"Platform Team","unitType":"team","managerUserId":"maya"}' | grep -o '"orgUnitId":"[^"]*"' | cut -d'"' -f4)

curl -s -X POST localhost:8085/api/v1/org-units/$B/reparent -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" -d "{\"newParentId\":\"$A\",\"hierarchyType\":\"reporting-line\"}"
curl -s "localhost:8085/api/v1/org-units/$A/descendants?hierarchyType=reporting-line" -H "Authorization: Bearer $ADMIN_TOKEN"   # -> [Engineering, Platform Team]

# Re-parent the whole subtree onto a different root — a bounded rewrite, not a migration project (BR-006):
curl -s -X POST localhost:8085/api/v1/org-units/$B/reparent -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" -d "{\"newParentId\":\"$D\",\"hierarchyType\":\"reporting-line\"}"
curl -s "localhost:8085/api/v1/org-units/$A/descendants?hierarchyType=reporting-line" -H "Authorization: Bearer $ADMIN_TOKEN"   # -> [Engineering] only
curl -s "localhost:8085/api/v1/org-units/$D/descendants?hierarchyType=reporting-line" -H "Authorization: Bearer $ADMIN_TOKEN"   # -> [Operations, Platform Team]
```

### Running the test suite

```bash
./gradlew :modules:assignment-enrollment:test
```

**Known local issue:** the Testcontainers-based integration test
(`EnrollmentControllerIntegrationTest`) fails on this machine with
`Could not find a valid Docker environment` — Testcontainers 1.20.1's bundled
Docker client doesn't negotiate correctly with this Docker Desktop version's
named-pipe transport (`docker context inspect` shows the active endpoint is
`npipe:////./pipe/dockerDesktopLinuxEngine`, not the classic default pipe).
The `docker` CLI itself works fine — this is specifically a Java-client/pipe
compatibility issue. The full flow (including both Kafka hops and the
signature tamper-check) was instead verified by running all four real
services against docker-compose's Postgres/Redpanda, driving them with
`curl`, and directly mutating rows in Postgres to prove the verifier isn't a
no-op. Fix candidates for later: enable "Expose daemon on
tcp://localhost:2375" in Docker Desktop settings and point `DOCKER_HOST` at
that, or run the test suite from inside WSL2 directly.

## Project layout

```
modules/
  common/                 shared kernel: TenantId, EventStore, GenericJdbcEventStore,
                           EventSourcedAggregate, JdbcOutboxStore + OutboxPoller (transactional outbox),
                           {Assessment,Enrollment}EventMessage (Published Language), Jwt.tenantId()/roles(),
                           OpaAuthorizer (queries OPA over HTTP)
  course-management/      plain CRUD Course service + Ch.12 §7 hash-addressed, insert-only content versioning
  assignment-enrollment/  event-sourced Enrollment aggregate + REST API + outbox-backed Kafka producer/consumer
  assessment/              event-sourced Assessment aggregate + REST API + outbox-backed Kafka producer
  certification/           event-sourced Certificate aggregate (pins content version + signs it) + REST API + Kafka consumer + local signing
  org-hierarchy/           plain CRUD OrgUnit service + Ch.19 §2 PostgreSQL closure-table hierarchy model (multiple concurrent hierarchy types, re-parenting)
infra/keycloak/           realm-export.json — auto-imported realm, client, tenant_id claim mapper, seeded users
infra/opa/policies/       authz.rego — tenant isolation + role-based restricted-action rules (Ch.17 ADR-028)
docker-compose.yml        Postgres, Redpanda, Keycloak, OPA (all used); MinIO (provisioned, unused)
docs/akb/                 the 50-chapter Architecture Knowledge Base
```

Every service has its own `infrastructure/SecurityConfig.kt`: requires a
valid Keycloak-issued JWT on every endpoint except `/actuator/**` (and, in
Certification's case, the two endpoints Ch.26 §6 requires to stay public).
The shared `Jwt.tenantId()` extension in `common` is the one place the
`tenant_id` claim is read, so every service extracts it identically. Each
service also has an `infrastructure/AuthorizationConfig.kt` providing an
`OpaAuthorizer` bean (shared class in `common`, just the base URL differs),
which every controller calls before mutating or reading a resource — see
the Rego policy for the actual allow/deny rules.

Each service owns its own Postgres **schema** on the one shared local
Postgres instance — genuine per-context data ownership (Ch.11) without
needing four separate database containers locally. Assessment and Enrollment
each also own an `outbox` table in their schema (see below).

### How the transactional outbox works

Assessment and Enrollment no longer call Kafka directly from the request
thread. Instead, each event's Published Language message is inserted into
that service's own `outbox` table **inside the same `@Transactional` method**
as the event-store append — so it commits or rolls back atomically with the
domain state, using plain `JdbcTemplate`/`DataSource` participation, no
special two-phase-commit machinery. A `@Scheduled` `OutboxPoller` (every
500ms) finds unpublished rows and sends them to Kafka, marking them published
only after a successful send. This trades a small amount of latency
(sub-second, observed) for the durability guarantee proven above under a real
broker outage. Certification doesn't need this yet — it only consumes, it
doesn't publish.

## Next increments, in order

1. **Wire Org Hierarchy into authorization** — the other four services'
   OPA checks still only know tenant + role, not org-unit membership. This
   is what closes the "manager can act on any resource in their tenant"
   gap named above: resources need an `org_unit_id`, callers need their
   org-unit membership resolved (from `managerUserId` or a real membership
   table, still undecided), and the Rego policy needs an `org_ok` rule
   checking descendant-of relationships via a `resource_org_unit` /
   `caller_org_units` input — likely by having each service query
   org-hierarchy's `/descendants` endpoint, or by pushing org data into OPA
   as a data document (avoids an HTTP call per authz check). This is the
   top item now.
2. Ch.19 §3/§4's event side: publish `OrgUnitChanged`/`OrgUnitReparented`
   to Kafka via the outbox pattern, and give some service a reason to
   consume them (ADR-032's cache/fallback pattern needs a real consumer to
   be worth building, not just a TTL number).
3. Multi-tenancy hybrid isolation (Ch.12 §2/Ch.18) — required before more
   than one real customer.
4. Real KMS integration (Ch.40 §3), replacing `.local-kms/` — required
   before any certificate here means anything legally.
5. Explicit outbox dedup/processed-message tracking on the consumer side,
   tightening the "guards happen to make this safe" idempotency story noted
   above into something more deliberate.
6. Learning Paths (Ch.21) as an actual multi-step concept, so the
   certificate's realized branch/step sequence (Ch.21 §7) has something real
   to record — content-version pinning is done, this is the remaining half
   of that chapter's requirement.
