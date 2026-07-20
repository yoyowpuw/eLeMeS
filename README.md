# elemes — Enterprise LMS

Implementation scaffold for the Architecture Knowledge Base at [`docs/akb/`](docs/akb/00-index.md).
Start there for the full architecture reasoning — this README covers only how to run
what exists today.

## What this is right now

**The full compliance-critical-tier golden path (Ch.11 §5) is closed, end to
end, with a real cryptographic proof at the finish line.** Four independent
Spring Boot services:

- **course-management** (`:8083`) — plain CRUD (Ch.10 §3: Supporting tier, no
  event sourcing). Called synchronously by Enrollment to validate a `courseId`
  actually exists (Ch.11 §3 Customer-Supplier relationship).
- **assignment-enrollment** (`:8081`) — event-sourced Enrollment aggregate
  (Ch.12 §5), full Ch.5 §4 state machine: `Assigned → InProgress →
  (AwaitingGrading ⇄ InProgress) → Completed`. Publishes every committed
  event to Kafka.
- **assessment** (`:8082`) — event-sourced Assessment aggregate (Ch.11 #9),
  auto-grades multiple-choice submissions, publishes `AssessmentSubmitted/
  Passed/Failed` onto Kafka.
- **certification** (`:8084`) — event-sourced Certificate aggregate (Ch.11
  #10, Restricted-Evidentiary per Ch.40 §2). Consumes Enrollment's
  `ContentCompleted`/`GradingPassed` events, issues a PKI-signed certificate
  (Ch.26 ADR-043), exposes independent verification, and supports append-only
  revocation (Ch.41 §3).

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

## Tech stack (per the AKB's ADRs)

| Layer | Choice | Local dev stand-in |
|---|---|---|
| Language/runtime | Kotlin/JVM, JDK 21 LTS (Ch.15 ADR-023) | — |
| Framework | Spring Boot 3.3 | — (not pinned by the AKB; chosen for this scaffold) |
| Database | PostgreSQL (Ch.12 ADR-016) | Docker, `postgres:16-alpine`, **one schema per service** (`enrollment`, `assessment`, `course_mgmt`, `certification`) — genuine per-context data ownership on one shared local instance |
| Event bus/store | Managed Kafka (Ch.15 ADR-024) | **Redpanda** — fully wired: assessment → enrollment → certification, published via a transactional outbox in each producer |
| Key management | Cloud HSM/KMS (Ch.40 ADR-066) | **Local RSA keypair**, file-persisted under `.local-kms/` (gitignored) — see deferred items |
| Object storage | S3-compatible (Ch.28 ADR-045) | MinIO in docker-compose — provisioned, still unused |
| Identity/Auth | Bought CIAM platform (Ch.16 ADR-026) | **Not implemented** — every request hits a hardcoded `default-tenant`, no auth at all |

## Deliberately deferred (read this before assuming a gap is a bug)

- **No real authentication/authorization.** Every request runs as tenant
  `default-tenant`, on every service. Not ASVS-conformant, not production-safe.
  Explicit, agreed trade-off to prove event sourcing and the event-bus
  architecture first.
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
- **Content/path version pinning (Ch.5 ADR-005, Ch.21 §7) is not modeled.**
  The certificate references `courseId` as a bare string because Course
  Management has no versioning yet — the single biggest fidelity gap versus
  what Chapter 26 actually specifies. Tracked, not silently dropped.
- **No idempotency-key handling** (Ch.13 §4) on mutation endpoints yet.
- **Single tenant, pooled-only.** Chapter 12 §2's hybrid silo/pool model and
  Chapter 18's control plane don't exist yet.
- **Testcontainers integration test is broken on this machine** — see below.

## Running locally

Prerequisites: Docker Desktop (WSL2 backend), JDK 21 (services target it via
Gradle toolchain regardless of your system `JAVA_HOME`).

```bash
# 1. Start infra
docker compose up -d postgres redpanda

# 2. Run all four services (each in its own terminal, or backgrounded)
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:course-management:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:assessment:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:assignment-enrollment:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:certification:bootRun
```

```bash
# 3. Course -> Enrollment -> Assessment -> Certificate, end to end
COURSE_ID=$(curl -s -X POST localhost:8083/api/v1/courses \
  -H "Content-Type: application/json" -d '{"code":"SEC-101","title":"Security Awareness"}' \
  | grep -o '"courseId":"[^"]*"' | cut -d'"' -f4)

ENR_ID=$(curl -s -X POST localhost:8081/api/v1/enrollments \
  -H "Content-Type: application/json" -d "{\"learnerId\":\"learner-1\",\"courseId\":\"$COURSE_ID\"}" \
  | grep -o '"enrollmentId":"[^"]*"' | cut -d'"' -f4)
curl -X POST localhost:8081/api/v1/enrollments/$ENR_ID/start

ASM_ID=$(curl -s -X POST localhost:8082/api/v1/assessments -H "Content-Type: application/json" -d "
{\"enrollmentId\":\"$ENR_ID\",\"courseId\":\"$COURSE_ID\",\"passingScore\":70,
 \"questions\":[{\"questionId\":\"q1\",\"text\":\"2+2?\",\"options\":[\"3\",\"4\"],\"correctOptionIndex\":1}]}" \
  | grep -o '"assessmentId":"[^"]*"' | cut -d'"' -f4)
curl -X POST localhost:8082/api/v1/assessments/$ASM_ID/submit \
  -H "Content-Type: application/json" -d '{"answers":{"q1":1}}'

# Two Kafka hops later (Assessment -> Enrollment -> Certification):
curl localhost:8084/api/v1/certificates/by-enrollment/$ENR_ID
# -> grab the certificateId, then:
curl localhost:8084/api/v1/certificates/{certificateId}/verify   # {"valid":true}
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
                           {Assessment,Enrollment}EventMessage (Published Language)
  course-management/      plain CRUD Course service
  assignment-enrollment/  event-sourced Enrollment aggregate + REST API + outbox-backed Kafka producer/consumer
  assessment/              event-sourced Assessment aggregate + REST API + outbox-backed Kafka producer
  certification/           event-sourced Certificate aggregate + REST API + Kafka consumer + local signing
docker-compose.yml        Postgres, Redpanda (both used); MinIO (provisioned, unused)
docs/akb/                 the 50-chapter Architecture Knowledge Base
```

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

1. **Content/path version pinning** (Ch.5 ADR-005, Ch.21 §7) — requires
   Course Management to actually version content, and Certification to
   reference a specific version rather than a bare `courseId`. Now the
   biggest remaining fidelity gap versus what Chapter 26 specifies.
2. Real auth (Ch.16), Authorization (Ch.17), full Org Hierarchy (Ch.19) —
   required before any real tenant data touches this.
3. Multi-tenancy hybrid isolation (Ch.12 §2/Ch.18) — required before more
   than one real customer.
4. Real KMS integration (Ch.40 §3), replacing `.local-kms/` — required
   before any certificate here means anything legally.
5. Explicit outbox dedup/processed-message tracking on the consumer side,
   tightening the "guards happen to make this safe" idempotency story noted
   above into something more deliberate.
