# elemes — Enterprise LMS

Implementation scaffold for the Architecture Knowledge Base at [`docs/akb/`](docs/akb/00-index.md).
Start there for the full architecture reasoning — this README covers only how to run
what exists today.

## What this is right now

**The full compliance-critical-tier golden path (Ch.11 §5) is closed, end to
end, with a real cryptographic proof at the finish line.** Six independent
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
- **tenant-provisioning** (`:8086`) — plain CRUD tenant registry (Ch.10 §3:
  Standard tier), the Tenancy & Provisioning control-plane context Ch.18
  names (Ch.11 #3). Owns tenant lifecycle (`PROVISIONING → ACTIVE →
  OFFBOARDED`, no path back) and pushes each transition straight into OPA's
  data API — every other service's authorization check consults that
  pushed status on every single request, so offboarding a tenant revokes
  its access everywhere immediately, not eventually.

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
11. **A manager really is limited to their own org subtree for
    certificate revocation — not just their whole tenant.** Closes half of
    the gap named right above this list, in "Deliberately deferred":
    `revoke_certificate` is now Ch.19-scoped for managers, not just
    tenant-checked. Built a real subtree (`Platform`, managed by `maya`,
    with `Frontend` underneath it) and an unrelated unit (`Marketing`,
    nobody's subtree), enrolled a learner in each with an `orgUnitId`, let
    both run the full golden path to a real issued certificate — confirming
    `orgUnitId` propagates correctly through two Kafka hops, from
    Enrollment's `EnrollmentEventMessage` all the way to
    `Certificate.orgUnitId` — then: `maya` revoking the `Frontend` learner's
    certificate gets 200 (it's in her subtree); `maya` revoking the
    `Marketing` learner's certificate gets 403 (it isn't, even though both
    are in the same tenant); `admin1` revoking that same `Marketing`
    certificate gets 200 (admin stays tenant-wide, unaffected by
    org-scoping). Every existing regression case was re-checked too:
    `learner1` still gets 403 outright, no token still gets 401, re-revoking
    an already-revoked certificate still 409s, and `/verify` stays public
    with no token even for a certificate a manager just revoked.
12. **Org-scoping extends past certificate revocation — course authoring
    and org re-parenting are subtree-limited for managers too now.**
    `create_course`, `publish_course_version`, and `org_unit_reparent` all
    got the same treatment as item #11. Proof, all against real services: a
    course tagged with `Frontend`'s `orgUnitId` (inside `maya`'s subtree)
    can be created and have a new version published by `maya` — 201 both
    times; the same two calls against a course tagged with an unrelated
    `Sales` unit both come back 403 for `maya`, and 201/201 for `admin1`; a
    course created with no `orgUnitId` at all stays open to `maya` (org-
    scoping is opt-in, never a blanket lockout). Re-parenting got the
    fullest test: `maya` moving a unit she *doesn't* manage (`Backend`)
    under one she does (`Platform`) is 403 — the *source* isn't in her
    scope; `maya` moving `Frontend` (which she does manage) under `Sales`
    (which she doesn't) is also 403 — the *destination* isn't in her scope
    either, so a manager can't "give away" part of the org into a hierarchy
    they don't control; `maya` moving `Frontend` under `Mobile` (both under
    `Platform`, both in scope) succeeds with 200; `admin1` moving two units
    neither in `maya`'s scope succeeds regardless, 200. `org_unit_create`
    was deliberately left *out* of this — a brand-new unit has no target org
    to scope against until a separate, already-scoped `reparent` call
    attaches it somewhere — confirmed `maya` can still create org units
    freely. Standard regression set re-checked clean: `learner1` still 403,
    no token still 401, plain tenant-scoped reads unaffected.
13. **Org Hierarchy now publishes real Kafka events, and Certification
    genuinely caches org-scope lookups instead of calling out on every
    single check — proven by actually killing org-hierarchy mid-test, not
    just by reading the code.** org-hierarchy publishes `OrgUnitChanged`
    (on creation) and `OrgUnitReparented` (on reparent) through the same
    transactional-outbox pattern as every other producer here. Certification
    now caches `my-scope` lookups for 5 minutes (Ch.19 ADR-032) instead of
    calling org-hierarchy on every `revoke_certificate` check, invalidated
    by a Kafka listener on those same events — proven end to end: `maya`
    revoked one certificate (a live call, populating her cache entry),
    org-hierarchy was then killed outright, and `maya` revoked a *second*
    in-scope certificate successfully anyway — served entirely from cache,
    with the dependency actually down, not just "should still work in
    theory". Org-hierarchy was restarted, a real org-unit creation was used
    to publish a real `OrgUnitChanged` event, and Certification's log
    confirmed it consumed that event and cleared the cache. Org-hierarchy
    was killed a second time with the cache now empty, and `maya` revoking
    a *third* certificate correctly got a `503` — "org-hierarchy is
    unreachable and no cached answer exists" — not a silent 200. This was a
    deliberate design choice, not a literal copy of ADR-032: the chapter's
    "never block, use last-known-good" framing was written for Assignment's
    *eligibility computation*, where staleness is a UX problem; reused
    naively for an *authorization* decision, a cache miss during an outage
    would have to fail open or closed, and failing open on "can this
    manager revoke this certificate" is a real vulnerability, not a
    convenience — so a miss with nothing cached denies with an explicit
    503, while a hit (fresh or, if the live refresh fails, stale-but-known)
    is served without ever touching the network. `admin1` was confirmed
    unaffected throughout — revocation as admin doesn't consult the cache
    or org-hierarchy at all, so it kept working the entire time
    org-hierarchy was down.
14. **The ADR-032 cache now covers Course Management's org-scoped actions
    too — and a real bug this testing style caught got fixed, not just
    logged as a known gap.** `create_course`/`publish_course_version` now
    have the same 5-minute cache + event-invalidation as certificate
    revocation, proven the same way: `maya` created and published a course
    tagged with her subtree live (populating the cache), org-hierarchy was
    killed, and a second create/publish pair against that same subtree
    succeeded entirely from cache; org-hierarchy was restarted, a real
    `OrgUnitChanged` event was triggered and confirmed consumed
    (course-management's log showed the invalidation); org-hierarchy was
    killed again and a third, never-cached create attempt correctly got
    503. Along the way, this outage testing surfaced a real bug rather than
    a hypothetical one: caller scope was being resolved (and thus
    org-hierarchy depended on) for *every* manager request regardless of
    whether the resource even had an `orgUnitId` — so creating a
    *completely unscoped* course, which the Rego policy already treats as
    trivially allowed via its null-handling, was still failing with 503
    during an org-hierarchy outage for no reason. Fixed by only resolving
    caller scope when the resource actually has an org unit to check
    against (same fix applied to Certification's `revoke_certificate`,
    verified by the same live course-management test but not independently
    re-run through a second full outage cycle there — identical code shape,
    already proven correct once). Deliberately NOT extended to
    org-hierarchy's own `reparent` check: that resolves scope in-process
    against its own database, not over the network, so caching it would add
    staleness risk (a manager who just lost authority over a unit could
    still reparent it for up to the cache's TTL) with no resilience benefit
    to offset it, since there's no outage to survive in the first place.
15. **PostgreSQL Row-Level Security is real, defense-in-depth beneath the
    application layer — proven by directly querying Postgres, not just by
    reading the policy SQL.** Ch.12 §2's pooled-cluster isolation model is
    now enforced by the database itself, not only by each controller's own
    query logic — which matters concretely here, since several repository
    methods (`CourseRepository.findById()` among them) never filter by
    `tenant_id` in their `WHERE` clause at all; they relied entirely on the
    OPA check running afterward. A real bug was caught building this, not
    just a hypothetical one: the app's own connecting role (`elemes`,
    `POSTGRES_USER`'s bootstrap account) turned out to be a Postgres
    **superuser**, and superusers bypass row security unconditionally —
    `FORCE ROW LEVEL SECURITY` is powerless against that, by Postgres
    design. First proof attempt against that role silently returned every
    row with no tenant context set at all. Fixed by adding a dedicated,
    non-superuser `elemes_app` role (`infra/postgres/init-app-role.sql`)
    that every service now actually connects as — re-tested and confirmed:
    connecting directly via `psql` as that exact role, with no
    `app.tenant_id` session variable set, returned **zero rows** for a
    course that had just been created moments earlier over the real API;
    setting it to the wrong tenant also returned zero rows and a raw
    `UPDATE` under the wrong tenant affected zero rows; setting it to the
    correct tenant returned the real data. The full golden path was re-run
    afterward to confirm nothing broke, including the one deliberate
    exception — Certification's `/verify` (Ch.26 §6, no token, therefore no
    tenant) calls `TenantContext.setBypass()`, its one legitimate call
    site, and still resolved a real certificate with no token at all. Kafka
    consumer threads (`EnrollmentEventListener`, `AssessmentEventListener`)
    needed their own fix too — they don't go through the HTTP request that
    normally sets tenant context, so each now sets it explicitly from the
    message's own `tenantId` field before touching the database.
    **One behavior change surfaced by this testing, not previously true:**
    a cross-tenant single-resource read (e.g. `globex-corp` reading an
    `acme-corp` course or org unit by ID) now returns **404, not 403** —
    RLS filters the row out before `repository.findById()` ever returns
    it, so the code never reaches the OPA check that used to be what
    produced the 403. Arguably a stronger property (a caller outside the
    tenant can no longer even prove the resource exists), but a genuine,
    observable change from what was documented in items #9–10 above for
    those specific single-resource GET endpoints.
16. **Offboarding a tenant revokes its access everywhere, immediately —
    proven by actually offboarding a real seeded tenant mid-session and
    watching every one of its callers get locked out at once, not by
    reading the policy.** `acme-corp` was registered into the new tenant
    registry (`PROVISIONING`), and — a real bootstrapping bug surfaced
    immediately — `admin1` couldn't even activate their own tenant, because
    the first cut of the `tenant_active` check applied to *every* action
    including the one meant to establish it, a deadlock no seeded tenant
    could ever escape. Fixed by exempting the four tenant-lifecycle actions
    themselves from the check (a real platform-ops identity would sit
    outside any tenant's own status entirely; this codebase has no such
    identity, so the exemption is scoped narrowly instead). A second bug
    surfaced right after, more subtle: the control plane pushed tenant
    status to OPA at `data.elemes.tenants.*` (matching the Rego file's own
    `package elemes.authz` name) while the policy read from `data.tenants`
    — two unrelated paths, a package declaration has no bearing on what a
    `data.X` reference elsewhere resolves to — so the very first test
    silently passed for the wrong reason (an always-undefined path reads as
    "unknown tenant, allow"), caught by querying OPA's raw data document at
    the exact path the policy reads and finding it empty. With both fixed:
    `acme-corp` was activated (real 200, real access restored), then
    offboarded — and immediately after, with no delay and no service
    restart, `admin1` got 403 creating a course, `admin1` got 403 reading a
    course they already owned, and `learner1` (a completely different role,
    same tenant) got 403 on the same read too — the block isn't role- or
    action-specific, it's tenant-wide. `globex-corp`, an entirely unrelated
    tenant, was unaffected throughout (still got a normal RLS-driven 404 on
    a course that was never theirs, not a 403). `acme-corp` was restored to
    `ACTIVE` directly afterward (the app's own state machine has no path
    back from `OFFBOARDED` by design — a direct DB/OPA fix, same pattern as
    other test-environment restores in this project, not a feature).
17. **A tenant's own admin genuinely cannot provision or offboard tenants
    anymore — including their own — only a separate platform-admin
    identity can, and that separation is enforced by OPA, not just by
    which buttons a UI happens to show.** Added a distinct Keycloak realm
    role (`platform-admin`) held by a dedicated `platform-ops` user with no
    real business tenant, and split what was one `tenant_read` action
    covering both single-tenant reads and the full registry listing into
    two: `tenant_list` (platform-admin only) and `tenant_read`
    (tenant-scoped self-read — a tenant's own `admin` can see their own
    record, `tenant_ok` blocks reading anyone else's). Proven against the
    real running services, not just the policy in isolation: `admin1`
    (acme-corp) got 403 trying to create a brand-new tenant, 403 trying to
    offboard their own tenant, and 403 trying to list the whole registry —
    but still 200 reading their own tenant's record. `platform-ops` created,
    activated, and offboarded a tenant, 200 the whole way, despite that
    role's own JWT `tenant_id` (`"platform"`) never matching any real
    business tenant — `tenant_ok` has an explicit bypass for
    `platform-admin` specifically, since that role is inherently
    cross-tenant by design, not scoped to any single one. `learner1` (no
    admin role at all) still got 403 outright, and no token still got 401.

## Tech stack (per the AKB's ADRs)

| Layer | Choice | Local dev stand-in |
|---|---|---|
| Language/runtime | Kotlin/JVM, JDK 21 LTS (Ch.15 ADR-023) | — |
| Framework | Spring Boot 3.3 | — (not pinned by the AKB; chosen for this scaffold) |
| Database | PostgreSQL (Ch.12 ADR-016) | Docker, `postgres:16-alpine`, **one schema per service** (`enrollment`, `assessment`, `course_mgmt`, `certification`, `org_hierarchy`) — genuine per-context data ownership on one shared local instance |
| Org hierarchy model | Closure table, PostgreSQL-native (Ch.19 ADR-031) | Implemented directly — no local stand-in needed, the AKB's selected technology *is* what's running locally |
| Tenancy control plane | Ch.18 ADR-029: separate control/data plane, single provisioning model | Implemented directly as `tenant-provisioning` — data plane routing is trivial locally (one pooled Postgres cluster), so this is mostly the registry/lifecycle half of ADR-029, not the multi-region routing half |
| Event bus/store | Managed Kafka (Ch.15 ADR-024) | **Redpanda** — fully wired: assessment → enrollment → certification, and org-hierarchy → certification (cache invalidation), all published via a transactional outbox in each producer |
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
- **Org Hierarchy is real, feeds four real authorization decisions, publishes
  real events, and two of three eligible consumers genuinely cache against
  them.** The closure-table model, re-parenting, and matrixed hierarchy
  types are tested (see "What's proven" #10); `revoke_certificate`,
  `create_course`, `publish_course_version`, and `org_unit_reparent` are
  all genuinely org-scoped for managers (#11–12); `OrgUnitChanged`/
  `OrgUnitReparented` publish over Kafka via the same outbox pattern as
  every other producer, and both Certification's and Course Management's
  `my-scope` lookups are cached for 5 minutes (Ch.19 ADR-032) and
  invalidated by those events, proven under actual org-hierarchy outages,
  not just described (#13–14). Scope resolution is also lazy in both
  services now — a resource with no `orgUnitId` never triggers a
  cache/org-hierarchy dependency at all, a real bug the outage testing
  caught (see #14). `org_unit_create` is the one deliberate authorization
  exception — a brand-new unit has no target org to scope against until a
  separate `reparent` call attaches it somewhere, so it stays tenant+role
  only by design, not by omission. `org_unit_reparent` is the one
  deliberate *caching* exception — org-hierarchy resolves its own scope
  in-process against its own database, not over the network, so there's no
  outage to survive and caching it would only add staleness risk. What's
  still not built: (1) org-unit membership itself stays a single
  `managerUserId` field on each unit — no real HRIS/directory sync behind
  any of it; (2) the cache invalidation is coarse (any org-unit event
  clears the *entire* cache, not just entries for managers actually
  affected by that specific change) — correct, since a false cache-clear
  just costs one extra live lookup, but not the fine-grained invalidation
  ADR-032 could in principle support.
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
- **Data-plane isolation (RLS), the control plane's core lifecycle, and its
  platform-vs-tenant identity separation are all real now; the silo tier
  and the provisioning workflow's config steps aren't.** Pooled tenants get
  database-enforced row isolation (What's proven #15); the control plane
  (Ch.18 — tenant registry, `PROVISIONING → ACTIVE → OFFBOARDED` lifecycle)
  genuinely revokes access platform-wide the instant a tenant is offboarded
  (#16); and a tenant's own `admin` genuinely cannot manage the tenant
  registry at all anymore — only a dedicated `platform-admin` Keycloak role
  can (#17). What's still not built: the **silo tier** (a dedicated cluster
  for tenants over the size/regulatory threshold) is metadata-only,
  `isolationTier: SILO` on a tenant record doesn't actually provision
  separate infrastructure anywhere; there's no SSO/SCIM/HRIS configuration
  step in the provisioning lifecycle (Ch.18 §4 names it, `activate()` just
  flips a status); and there's genuinely only two tenants' worth of real
  business data in this environment regardless (`acme-corp`/`globex-corp`,
  both in the same pooled cluster) — `platform-ops` isn't a business
  tenant, it's the platform identity that manages the registry itself.
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

**A Postgres RLS gotcha that's easy to miss entirely:** `FORCE ROW LEVEL
SECURITY` on a table does **not** apply to a Postgres superuser, no matter
what — superusers bypass row security unconditionally, by design, and
there is no table-level setting that overrides that. The Docker Postgres
image's `POSTGRES_USER` bootstrap account **is** a superuser by default.
If every service connects as that account (as this repo originally did),
every RLS policy in it is a no-op that looks correct in the migration SQL
and in `\d+ tablename`, and will keep looking correct until someone
actually queries the table directly as that role and notices all tenants'
rows come back. Fixed here by adding a dedicated non-superuser
`elemes_app` role (`infra/postgres/init-app-role.sql`) that every service
connects as instead — `select rolsuper, rolbypassrls from pg_roles` is the
one-line check that would have caught this immediately, worth running
before trusting any RLS setup against a role you didn't create yourself.

**An OPA data-API gotcha, the second one this project has hit:** a Rego
file's `package elemes.authz` declaration only controls where **that
file's own rules** live in the data tree (`data.elemes.authz.*`) — it has
zero effect on what a plain `data.X` reference *inside* a rule body
resolves to, since `data` references are always absolute from the root.
Pushing external data to `PUT /v1/data/elemes/tenants/<id>` while a rule
reads `data.tenants[...]` (no `elemes` prefix) means the rule is reading a
path nothing ever writes to — permanently undefined, which read as
"unknown tenant" in this policy's fallback logic and let every request
through regardless of actual tenant status. No error, no warning — it just
silently does the wrong thing forever. Caught by querying OPA's raw data
document (`GET /v1/data/tenants/<id>`) directly and finding it empty right
after a push that should have populated it; worth doing before trusting
any policy branch that depends on externally-pushed data, the same way the
existing Rego-vs-Kotlin gotcha above says to test the policy directly
before suspecting the calling code.

## Running locally

Prerequisites: Docker Desktop (WSL2 backend), JDK 21 (services target it via
Gradle toolchain regardless of your system `JAVA_HOME`).

```bash
# 1. Start infra (Keycloak's realm/client/users auto-import at startup —
#    give it a few seconds before requesting a token). On a genuinely fresh
#    postgres_data volume this also auto-creates the elemes_app role every
#    service connects as (infra/postgres/init-app-role.sql) — on an EXISTING
#    volume from before this role existed, run this once manually instead:
#    docker exec elemes-postgres psql -U elemes -d elemes -c \
#      "create role elemes_app with login password 'elemes_app_local_dev'; \
#       grant create, connect on database elemes to elemes_app;"
docker compose up -d postgres redpanda keycloak opa

# 2. Run all six services (each in its own terminal, or backgrounded)
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:course-management:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:assessment:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:assignment-enrollment:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:certification:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:org-hierarchy:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:tenant-provisioning:bootRun
```

```bash
# 3. Get a real token (see infra/keycloak/realm-export.json for the seeded
#    users — learner1/admin1 in tenant "acme-corp", plus maya, a "manager"-
#    role user in the same tenant, a second-tenant user, and platform-ops,
#    a "platform-admin"-role user with no real business tenant)
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

# 7. Manager-scoped certificate revocation: maya can only revoke certificates
# for learners in her own subtree (Platform Team, under D), not anyone else's.
MAYA_TOKEN=$(curl -s -X POST http://localhost:8080/realms/elemes/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=elemes-service" -d "username=maya" -d "password=maya" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
curl -s "localhost:8085/api/v1/org-units/my-scope?hierarchyType=reporting-line" -H "Authorization: Bearer $MAYA_TOKEN"   # -> [Platform Team, ...]

# Enroll a learner into $B (in maya's scope) and another into an unrelated unit,
# run each through to a real certificate (see step 4's golden path), then:
curl -i -X POST localhost:8084/api/v1/certificates/{inScopeCertId}/revoke \
  -H "Content-Type: application/json" -H "Authorization: Bearer $MAYA_TOKEN" -d '{"reason":"test"}'   # -> 200
curl -i -X POST localhost:8084/api/v1/certificates/{outOfScopeCertId}/revoke \
  -H "Content-Type: application/json" -H "Authorization: Bearer $MAYA_TOKEN" -d '{"reason":"test"}'   # -> 403
curl -i -X POST localhost:8084/api/v1/certificates/{outOfScopeCertId}/revoke \
  -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" -d '{"reason":"test"}'  # -> 200 (admin stays tenant-wide)
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
                           {Assessment,Enrollment,OrgUnit}EventMessage (Published Language), Jwt.tenantId()/roles(),
                           OpaAuthorizer (queries OPA over HTTP), TenantContext + TenantAwareDataSource +
                           TenantContextFilter + TenantDataSourceConfig (Ch.12 §2 Postgres RLS wiring)
  course-management/      plain CRUD Course service + Ch.12 §7 hash-addressed, insert-only content versioning + OrgScopeCache (Ch.19 ADR-032, 5-min TTL, fail-closed) + Kafka consumer for cache invalidation
  assignment-enrollment/  event-sourced Enrollment aggregate + REST API + outbox-backed Kafka producer/consumer
  assessment/              event-sourced Assessment aggregate + REST API + outbox-backed Kafka producer
  certification/           event-sourced Certificate aggregate (pins content version + signs it) + REST API + 2 Kafka consumers (EnrollmentEventMessage, OrgUnitEventMessage) + local signing + OrgScopeCache (Ch.19 ADR-032, 5-min TTL, fail-closed)
  org-hierarchy/           plain CRUD OrgUnit service + Ch.19 §2 PostgreSQL closure-table hierarchy model (multiple concurrent hierarchy types, re-parenting) + outbox-backed Kafka producer
  tenant-provisioning/     plain CRUD tenant registry (Ch.18 control plane) + OpaDataPusher (pushes lifecycle status into OPA's data API)
infra/keycloak/           realm-export.json — auto-imported realm, client, tenant_id claim mapper, seeded users
infra/opa/policies/       authz.rego — tenant isolation + role-based restricted-action rules (Ch.17 ADR-028)
infra/postgres/           init-app-role.sql — creates the non-superuser elemes_app role RLS depends on (Ch.12 §2)
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

1. Real KMS integration (Ch.40 §3), replacing `.local-kms/` — required
   before any certificate here means anything legally. This is the top item
   now — the platform-ops identity gap named in the previous increment is
   closed (`platform-admin` role, see "What's proven" #17).
2. Explicit outbox dedup/processed-message tracking on the consumer side,
   tightening the "guards happen to make this safe" idempotency story noted
   above into something more deliberate.
3. Learning Paths (Ch.21) as an actual multi-step concept, so the
   certificate's realized branch/step sequence (Ch.21 §7) has something real
   to record — content-version pinning is done, this is the remaining half
   of that chapter's requirement.
4. The silo tier (Ch.12 §2/Ch.18 §3) as more than tenant metadata — actually
   provisioning a dedicated cluster for a tenant crossing the threshold is
   still entirely unmodeled.
