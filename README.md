# eLeMeS — Enterprise Learning Management System

## What is eLeMeS?

eLeMeS is an enterprise-grade, multi-tenant Learning Management System —
the reference implementation of a 50-chapter Architecture Knowledge Base
(AKB) covering the full design of a Fortune-500-scale LMS: course
authoring, learner enrollment, assessment and grading, evidentiary
(PKI-signed) certification, multi-step learning paths, organizational
hierarchy, and hybrid pooled/dedicated multi-tenancy.

It's built as a set of independently deployable Spring Boot services
following Domain-Driven Design (DDD) bounded contexts, using event
sourcing for the compliance-critical parts of the system (enrollment,
assessment, certification) and a transactional outbox for reliable
cross-service messaging over Kafka.

The full architecture reasoning — every decision, alternative considered,
and trade-off — lives in [`docs/akb/`](docs/akb/00-index.md). This README
is the practical companion: what exists, how it fits together, and how to
run it.

## Modules

| Service | Port | What it does |
|---|---|---|
| **frontend** | `:5173` | React web app — course browsing/authoring, enrollment, assessments, and certificate issuance/verification, signed in via Keycloak. |
| **course-management** | `:8083` | Course authoring and content versioning (each publish creates a new, immutable content version rather than editing in place). Also owns **Learning Paths** — ordered, multi-step sequences of courses, versioned the same way. |
| **assignment-enrollment** | `:8081` | Enrollment lifecycle (`Assigned → InProgress → AwaitingGrading → Completed`), pinning the exact content version a learner enrolled against. Drives learners through multi-step Learning Paths automatically as each step completes. |
| **assessment** | `:8082` | Multiple-choice assessments with auto-grading. |
| **certification** | `:8084` | Issues cryptographically signed (PKI) certificates on course/path completion, pinned to the exact content version and — for a Learning Path — the realized sequence of steps actually completed. Certificates are independently verifiable by anyone, with no platform access, and support append-only revocation. |
| **org-hierarchy** | `:8085` | Organizational structure as a PostgreSQL closure table — a unit can sit in multiple independent hierarchies at once (e.g. reporting line vs. cost center), and re-parenting a subtree is a single bounded transaction, never a migration. |
| **tenant-provisioning** | `:8086` | The multi-tenancy control plane: tenant lifecycle (`PROVISIONING → ACTIVE → OFFBOARDED`) and isolation-tier provisioning. |

## Key capabilities

- **Event-sourced core** — Enrollment, Assessment, and Certificate are
  event-sourced aggregates (their state is a fold over an immutable event
  log), giving a genuine audit trail for the compliance-critical parts of
  the system.
- **Reliable cross-service messaging** — every cross-service event is
  written to a transactional outbox in the same database transaction as
  the domain state, then published by a background poller — no
  publish-then-hope, survives a broker outage with zero data loss.
- **Real authentication & authorization** — every endpoint requires a
  valid OIDC token (Keycloak); every mutation is checked against a shared
  policy-as-code engine (Open Policy Agent) for both role and
  cross-tenant checks, plus org-hierarchy-scoped authorization for
  managers.
- **Multi-tenant data isolation, two ways** — small/medium tenants share
  a pooled database with PostgreSQL Row-Level Security enforcing
  isolation at the database layer; large or regulated tenants get a
  genuinely separate, dedicated database, provisioned automatically and
  routed to transparently.
- **Evidentiary certificates** — certificates are digitally signed via a
  KMS-backed signing service, independently verifiable by any third
  party, tamper-evident, and support key rotation without invalidating
  previously issued certificates.
- **Learning Paths** — multi-step, ordered course sequences; a learner's
  realized path (which steps they actually completed) is signed into
  their final certificate.

## Tech stack

| Layer | Choice | Local dev stand-in |
|---|---|---|
| Language/runtime | Kotlin/JVM, JDK 21 LTS | — |
| Framework | Spring Boot 3.3 | — |
| Database | PostgreSQL | Docker `postgres:16-alpine` — one schema per service on a shared pooled instance, plus a second instance for dedicated (silo) tenant databases |
| Org hierarchy model | Closure table, PostgreSQL-native | Implemented directly |
| Event bus | Managed Kafka | Redpanda (Kafka-API-compatible, single binary) |
| Key management | Cloud HSM/KMS | HashiCorp Vault (Transit secrets engine) |
| Object storage | S3-compatible | MinIO (provisioned, not yet used) |
| Identity/Auth | CIAM platform | Keycloak (OIDC, custom `tenant_id` claim) |
| Authorization | Policy-as-code | Open Policy Agent (Rego policy) |
| Frontend | React | Vite + TypeScript, React Router, React Query, real OIDC Authorization Code + PKCE login against Keycloak |

## Project layout

```
frontend/                  React SPA — see its own section below
modules/
  common/                 shared kernel: event store, transactional outbox, tenant context/routing,
                           OPA client, Published Language DTOs
  course-management/      Course + Learning Path (plain CRUD, content/path versioning)
  assignment-enrollment/  event-sourced Enrollment aggregate + Learning Path progression
  assessment/             event-sourced Assessment aggregate
  certification/          event-sourced Certificate aggregate, KMS-backed signing
  org-hierarchy/          closure-table org structure
  tenant-provisioning/    tenant lifecycle + isolation-tier provisioning (control plane)
infra/
  keycloak/               realm export — client, tenant_id claim mapper, seeded users
  opa/policies/           authz.rego — tenant isolation + role-based authorization rules
  postgres/               non-superuser app-role bootstrap (required for Row-Level Security)
  vault/                  signing-key ACL policy + one-time bootstrap script
docker-compose.yml        Postgres (pooled + silo), Redpanda, Keycloak, OPA, Vault, MinIO
docs/akb/                 the 50-chapter Architecture Knowledge Base
```

## Running locally

**Prerequisites:** Docker Desktop, JDK 21 (Gradle toolchain targets it
regardless of your system `JAVA_HOME`).

```bash
# 1. Start infrastructure
docker compose up -d postgres postgres-silo redpanda keycloak opa vault

# 2. One-time Vault setup (dev-mode Vault keeps everything in memory, so
#    this needs re-running whenever the vault container is recreated)
bash infra/vault/bootstrap.sh
# Prints a role_id/secret_id pair — if different from what's committed in
# modules/certification/src/main/resources/application.yml, update it there.

# 3. Run all six services (each in its own terminal, or backgrounded)
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:course-management:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:assessment:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:assignment-enrollment:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:certification:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:org-hierarchy:bootRun
JAVA_HOME="/path/to/jdk-21" ./gradlew :modules:tenant-provisioning:bootRun

# 4. Run the frontend
cd frontend && npm install && npm run dev
# -> http://localhost:5173 — sign in with any seeded user (see below)
```

```bash
# 5. Or drive the API directly: get a token (see infra/keycloak/realm-export.json for seeded users)
TOKEN=$(curl -s -X POST http://localhost:8080/realms/elemes/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=elemes-service" \
  -d "username=learner1" -d "password=learner1" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

# Course -> Enrollment -> Assessment -> Certificate, end to end
COURSE_ID=$(curl -s -X POST localhost:8083/api/v1/courses \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"code":"SEC-101","title":"Security Awareness","initialContentHash":"sha256-placeholder-v1"}' \
  | grep -o '"courseId":"[^"]*"' | cut -d'"' -f4)

ENR_ID=$(curl -s -X POST localhost:8081/api/v1/enrollments \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d "{\"learnerId\":\"learner1\",\"courseId\":\"$COURSE_ID\"}" \
  | grep -o '"enrollmentId":"[^"]*"' | cut -d'"' -f4)
curl -X POST localhost:8081/api/v1/enrollments/$ENR_ID/start -H "Authorization: Bearer $TOKEN"

ASM_ID=$(curl -s -X POST localhost:8082/api/v1/assessments -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "
{\"enrollmentId\":\"$ENR_ID\",\"courseId\":\"$COURSE_ID\",\"passingScore\":70,
 \"questions\":[{\"questionId\":\"q1\",\"text\":\"2+2?\",\"options\":[\"3\",\"4\"],\"correctOptionIndex\":1}]}" \
  | grep -o '"assessmentId":"[^"]*"' | cut -d'"' -f4)
curl -X POST localhost:8082/api/v1/assessments/$ASM_ID/submit \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{"answers":{"q1":1}}'

# Two Kafka hops later (Assessment -> Enrollment -> Certification):
curl localhost:8084/api/v1/certificates/by-enrollment/$ENR_ID -H "Authorization: Bearer $TOKEN"
# -> grab the certificateId, then verify WITHOUT a token — deliberately public:
curl localhost:8084/api/v1/certificates/{certificateId}/verify   # {"valid":true}
```

### Running the test suite

```bash
./gradlew :modules:assignment-enrollment:test
```
