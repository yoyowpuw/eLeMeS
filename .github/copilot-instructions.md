# Copilot Instructions — elemes (Enterprise LMS)

This file is auto-loaded by GitHub Copilot Chat as repository context. Read
it fully before writing or suggesting any code in this repo.

## What this repository is

An implementation of an Enterprise LMS, built strictly against a pre-written
50-chapter Architecture Knowledge Base (AKB) at
[`docs/akb/00-index.md`](../docs/akb/00-index.md). Every architectural
decision (technology choices, data models, service boundaries) is already
made and recorded there as numbered ADRs (`ADR-001`...`ADR-088`) and
numbered requirements (`BR-###`, `FR-###`, `NFR-###`). **Do not re-derive or
second-guess these decisions. Look them up and follow them.**

[`README.md`](../README.md) at the repo root is the current implementation
status: what's built, what's proven (with how it was verified), what's
deliberately deferred and why, and the next-increments list in priority
order. **Read the README before doing anything — it is more current than
your training data and more current than this file.**

## Hard rules — do not deviate from these without being explicitly told to

1. **Follow the next-increments list in `README.md`, in order.** Do not jump
   ahead to a later item (e.g., don't start on auth/authorization while
   content-version-pinning is still open) unless the user explicitly asks
   for something else. If the user's request doesn't match the top of the
   list, do what they ask, but say so — don't silently reorder priorities.
2. **Every new bounded context/service must cite the AKB chapter/ADR it
   implements** in a code comment at the top of its main config or aggregate
   class, the same way existing modules do (e.g.,
   `modules/assessment/build.gradle.kts` cites "Ch.11 #9 Assessment &
   Question Bank"). This is not decoration — it's how future readers (human
   or AI) verify the code still matches the spec.
3. **Compliance-critical-tier contexts are event-sourced** (Ch.11 §5:
   Assignment & Enrollment, Assessment & Question Bank, Certification &
   Compliance). Everything else is plain CRUD (Ch.10 §3: Supporting/Generic
   tiers). Do not event-source a Supporting-tier service; do not make a
   compliance-critical aggregate plain-CRUD.
4. **Cross-service publishing goes through the transactional outbox
   pattern**, not a direct `KafkaTemplate.send()` from a request thread. Use
   `com.elemes.common.JdbcOutboxStore` + `com.elemes.common.OutboxPoller`,
   the same way `assessment` and `assignment-enrollment` already do. This was
   specifically hardened after proving the direct-send approach silently
   drops events on a broker outage — see README "What's proven" item 6.
   Don't regress to direct sends for a new service.
5. **Cross-context event contracts are Published Language DTOs in
   `modules/common`**, never a consuming service importing another service's
   internal domain event classes. See `AssessmentEventMessage` /
   `EnrollmentEventMessage` for the pattern. A context's internal event
   vocabulary (e.g., `GradingPassed` in Enrollment) is never the same name as
   the upstream event it's reacting to (e.g., `AssessmentPassed`) — that
   naming discipline is intentional (Ch.10 §4 anti-corruption layer), not
   inconsistency to "fix."
6. **Every service owns its own Postgres schema** on the one shared local
   instance (`enrollment`, `assessment`, `course_mgmt`, `certification`, ...),
   configured via `spring.flyway.schemas` / `?currentSchema=` in
   `application.yml`. A new service gets a new schema name, not a shared one.
7. **Every table with a `tenant_id` column needs a `tenant_id`-leading
   composite index** on any hot query path (Ch.15 §8 binding rule). Don't
   add tenant-scoped tables without this.
8. **Idempotent consumption**: Kafka is at-least-once. New `@KafkaListener`
   methods must tolerate redelivery — either via aggregate state-machine
   guards (`check()` throwing `IllegalStateException` on a no-op re-apply, as
   in `AssessmentEventListener`) or an explicit uniqueness constraint (as in
   `certificate_projection.enrollment_id UNIQUE`). Don't add a consumer that
   would double-process or crash on redelivery.
9. **Be honest about what's stubbed or simplified**, in code comments and in
   the README's "Deliberately deferred" section — this codebase's convention
   is to flag every known gap explicitly (auth is fake, the KMS is a local
   file, Question Bank is inlined, etc.) rather than let it look finished
   when it isn't. Keep that habit for anything new you add.
10. **JDK 21 LTS, not whatever `JAVA_HOME` happens to point at globally.**
    Gradle toolchain in the root `build.gradle.kts` should keep pinning this;
    don't relax it to "whatever's on PATH."

## Current state (summary — README.md is authoritative, this may lag)

Four services, fully wired, proven end-to-end including a real broker-outage
recovery test:

- `course-management` (`:8083`) — plain CRUD, Ch.10 §3 Supporting tier.
- `assignment-enrollment` (`:8081`) — event-sourced, Ch.5 §4 state machine,
  outbox-backed Kafka producer + consumer.
- `assessment` (`:8082`) — event-sourced, auto-grades multiple choice,
  outbox-backed Kafka producer.
- `certification` (`:8084`) — event-sourced, PKI-signed certificates
  (local-file key, not real KMS), Kafka consumer, append-only revocation.

Shared kernel in `modules/common`: `TenantId`, `EventStore` +
`GenericJdbcEventStore`, `EventSourcedAggregate<E>`, `JdbcOutboxStore` +
`OutboxPoller`, and the Published Language DTOs
(`AssessmentEventMessage`, `EnrollmentEventMessage`).

No auth, no multi-tenancy beyond a hardcoded `default-tenant`, no real KMS —
all explicit, all documented, all next on the list.

## Next task, in priority order (do #1 unless told otherwise)

1. **Content/path version pinning** (Ch.5 ADR-005, Ch.21 §7, Ch.12 §7). This
   is the current top item — see the detailed brief below.
2. Real auth (Ch.16 — buy a CIAM platform, don't build one) + Authorization
   (Ch.17 — OPA-class policy-as-code) + full Org Hierarchy (Ch.19 —
   closure-table model). Required before any real tenant data touches this.
3. Multi-tenancy hybrid isolation (Ch.12 §2 pool/silo model, Ch.18 control
   plane). Required before more than one real customer.
4. Real KMS integration (Ch.40 §3), replacing `.local-kms/`.
5. Explicit outbox consumer-side dedup/processed-message tracking, tightening
   the current "guards happen to make this safe" idempotency story.

### Brief for task #1 — Content/path version pinning

Problem: `certification`'s `CertificateIssued` event currently pins a bare
`courseId` string (see `modules/certification/src/main/kotlin/com/elemes/certification/CertificateEvent.kt`
— the code comment there already flags this exact gap). Chapter 5 ADR-005
and Chapter 21 §7 require a certificate to pin the *exact content version*
(and realized path/branch, if applicable) a learner actually completed, not
just a course identifier, because content can be edited after a learner
finishes it and the certificate must remain evidentiary proof of what was
actually consumed.

What this requires, in order:
1. `course-management` needs a `ContentVersion` concept per Ch.12 §7:
   content is hash-addressed and insert-only — a new version is a new row, a
   `Course` has a "current version" but old versions remain queryable by ID.
   This is a real schema change to `course-management`, not just an API
   addition.
2. `assignment-enrollment`'s `Enrollment` aggregate needs to pin the
   `contentVersionId` it was enrolled against at `LearnerEnrolled` time
   (mirroring how `Ch.21 ADR-034`/this codebase's own Enrollment pins
   `courseId` at enrollment time already — same pattern, one more field).
3. That `contentVersionId` needs to flow through the Published Language
   (`EnrollmentEventMessage`) so Certification receives it.
4. `certification`'s `CertificateIssued` event and `CertificatePayload.canonical()`
   need the version ID added to what's stored *and signed* — changing the
   signed payload shape is a breaking change for any certificates already
   issued locally; that's fine for local dev data, just don't be surprised
   by it.

Do not attempt to build a full Question Bank or full SCORM/xAPI import
(Ch.22/24) as part of this — those remain out of scope per the README's
"Deliberately deferred" list. This task is specifically about version
*identity*, not full content management.

When done: update `README.md`'s "What's proven" list with how you verified
it (this codebase's convention — see items 1-6 there for the expected style:
concrete commands run, concrete output observed, not just "implemented"),
move this item off the deferred list, and renumber the next-increments list.
