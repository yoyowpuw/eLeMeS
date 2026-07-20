# Chapter 39 — DevOps

> Part VIII — Operations · [Index](../00-index.md) · Previous: [Ch. 38 — Observability](38-observability.md) · Next: Ch. 40 — Security

## 1. Purpose

This chapter has been named as the enforcement mechanism for more carried-forward
obligations than any other in the AKB. It selects the CI/CD platform and consolidates every
CI/CD gate promised across Parts II–VII into one canonical pipeline definition.

## 2. CI/CD Platform — Technology Evaluation

| Dimension | Self-hosted Jenkins | **Git-native CI/CD (GitHub Actions/GitLab CI-class), Selected** | Cloud-native build service (e.g., AWS CodePipeline-class) |
|---|---|---|---|
| Fit to 17-service macroservice topology (Ch.9) | Achievable but high configuration/maintenance burden per pipeline | **Native per-repo/per-service pipeline definitions, low incremental cost per new service** | Achievable, tighter cloud-provider coupling |
| Vendor lock-in (Ch.1 Principle 5) | Low (self-hosted) but high maintenance burden trade-off | **Low-moderate — pipeline-as-code (YAML) is broadly portable in concept even if not verbatim** | Higher — deeper cloud-provider-specific coupling |
| Hiring pool / ecosystem maturity | Large but legacy-skewed | **Very large, modern default for most engineers hired post-2018** | Large within that cloud's ecosystem specifically |
| Final Recommendation | Rejected — maintenance burden not justified given git-native alternatives | **Selected** | Rejected as primary — acceptable as a secondary deployment-target integration, not the pipeline's control plane |

## 3. Consolidated CI/CD Gate Registry

Every gate promised in a prior chapter is enforced here as a required, blocking pipeline
stage — this table is the canonical cross-reference future chapters must append to, not
duplicate:

| Gate | Source Chapter | Enforcement |
|---|---|---|
| Accessibility linting (axe-core-class) | Ch.7 NFR-032, Ch.14 | Blocking on frontend PRs |
| i18n hardcoded-string lint | Ch.14 Risk | Blocking on frontend PRs |
| Tenant cross-isolation test suite | Ch.7 NFR-021, Ch.12 | Blocking on every deployment, zero-tolerance per NFR-021 |
| RLS composite-indexing schema-review checklist | Ch.12, Ch.15 §8 | Blocking on schema-migration PRs |
| BFF-thin dependency-graph lint (no domain-rule imports in BFF code) | Ch.15 §7 | Blocking on BFF-service PRs |
| Contract testing (Public API vs. internal gRPC contracts) | Ch.13 Risk | Blocking on API-contract-affecting PRs |
| Policy-bundle atomic rollout (canary + rollback) | Ch.17 §6 (Blue Team addendum) | Required deployment strategy for policy-bundle changes specifically |
| Vulnerability remediation SLA (72h critical / 7d high / 30d medium) | Ch.7 NFR-020 | Tracked, not blocking per se, but escalated per SLA breach |
| Compliance-tier dependency-direction lint (no sync calls to standard tier from compliance tier, per corrected Ch.15 rule) | Ch.15 §9 | Blocking on compliance-tier-service PRs |

## 4. Deployment Strategy

Canary deployment (small-percentage traffic shift, automated rollback on error-rate/latency
regression) for all 17 services, with **stricter canary thresholds for the compliance-tier
services** (Ch.15 §4) consistent with their tighter SLA — operationalizing the dual-tier
distinction at the deployment-pipeline level, not just the runtime-topology level.

## Summary
Git-native CI/CD (not self-hosted Jenkins or a cloud-native-proprietary service) is
selected for its low incremental per-service cost at Chapter 9's 17-service scale and
broad hiring-pool alignment. This chapter consolidates nine previously-scattered CI/CD gate
obligations from Chapters 7, 12–15, and 17 into one canonical, blocking pipeline registry,
and extends the compliance-tier/standard-tier distinction into deployment canary
thresholds.

## Open Questions
Specific canary threshold percentages/durations — implementation-phase tuning, informed by real production error-rate baselines once available.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Growing gate registry (§3) increases pipeline duration, creating pressure to skip/bypass gates under deadline pressure | High — this is precisely the failure mode Chapter 7's own Risk register warned about | Medium-High | Gates must run in parallel where possible (not serially) to bound total pipeline time; "skip gate" requires explicit, logged, senior-approved override — never a silent default |
| New chapters (Ch.40+) will add further obligations not yet reflected here | Low (expected, not a defect) | High | This registry (§3) is explicitly living, per the same discipline as Ch.8's and Ch.35's registries |

## Architecture Decisions
**ADR-063: Git-native CI/CD platform, not self-hosted Jenkins or cloud-proprietary service** — §2. **ADR-064: Consolidated, blocking CI/CD gate registry with compliance-tier-specific stricter deployment thresholds** — §3–4.

## Future Research
Canary threshold tuning (implementation phase, post-launch data).

## Cross References
[Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-020, 021, 032) · [Ch. 9](../part-2-system-domain-architecture/09-product-architecture.md) · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md), [Ch. 13](../part-2-system-domain-architecture/13-api-strategy.md), [Ch. 14](../part-2-system-domain-architecture/14-frontend-architecture.md), [Ch. 15](../part-2-system-domain-architecture/15-backend-architecture.md), [Ch. 17](../part-3-identity-organization/17-authorization.md) (gate sources) · [Ch. 40](40-security.md)

## Definition of Done
- [x] CI/CD platform selected via Technology Evaluation Template
- [x] Nine prior-chapter gate obligations consolidated into one canonical registry
- [x] Deployment strategy specified with compliance-tier-specific thresholds

## Confidence Level
**High** — platform selection is a well-established industry default at this team scale; consolidation work directly operationalizes already-approved obligations rather than introducing new judgment calls.

## 5. Chapter Review

**Red Team:** The "explicit, logged, senior-approved override" mechanism for skipping a
gate (Risk mitigation) is a process control, not a technical one — nothing in this
chapter's architecture actually prevents a determined engineer with sufficient permissions
from disabling a gate outright in the pipeline configuration itself.

**Blue Team:** Accepted — valid limitation of any CI/CD gate design; no purely technical
control can fully substitute for organizational governance here. Addendum: pipeline
configuration changes that modify or remove a gate must themselves require a
**second-approver review** (a lightweight branch-protection rule), making silent gate
removal require collusion rather than a single actor's action — narrowing, though not
eliminating, the gap the Red Team identified; full elimination is a governance matter for
[Ch. 47 — Governance](../part-9-governance-future/47-governance.md), not purely a DevOps architecture concern.

**CTO:** ADR-064 **Approved with Conditions** — condition is the second-approver
branch-protection requirement for pipeline-configuration changes is binding; residual
governance responsibility explicitly handed to [Ch. 47](../part-9-governance-future/47-governance.md).

---
*End of Chapter 39. Proceed to Chapter 40 — Security.*
