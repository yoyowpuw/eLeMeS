# Chapter 36 — Mobile Strategy

> Part VII — Platform & Integration · [Index](../00-index.md) · Previous: [Ch. 35 — Integration Architecture](35-integration-architecture.md) · Next: Ch. 37 — Offline Learning

## 1. Purpose

Per Chapter 14's CTO condition, this chapter independently evaluates mobile technology
rather than treating React Native as pre-decided by Chapter 14's forward-looking bet. This
is the most consequential chapter for Frontline Fiona (Ch.4 §3.1), the platform's
highest-volume (P0) persona.

## 2. Mobile Technology — Independent Technology Evaluation

| Dimension | Native (separate Swift/Kotlin) | **React Native, Selected** | Flutter | PWA-only (no native app) |
|---|---|---|---|---|
| Performance for this app's profile (content/forms/video, not graphics-intensive) | Best, but the margin over RN/Flutter is not meaningful for this workload | Sufficient — well-proven at enterprise scale for exactly this app shape | Sufficient, arguably smoother UI consistency | Weakest — background sync and reliability constraints, especially historically on iOS Safari |
| Fit to NFR-033 (Fiona's ≤3-tap resume) | Achievable | Achievable | Achievable | Harder — PWA install/re-engagement friction works against this target |
| Offline capability (feeds Ch.37) | Full native control | Strong ecosystem (SQLite/Realm-class local stores, background sync libraries) | Strong, comparable to RN | Weakest — the reason this option is evaluated but not favored |
| Code-sharing with Ch.14's React web frontend | None | **Genuine — shared business-logic patterns, shared team skill set, validating Ch.14's bet independently** | None (Dart, separate from Ch.14's TypeScript/React stack) | N/A |
| Cost (Ch.1 Principle 6, two native codebases vs. one cross-platform) | Highest — effectively 2x engineering investment for iOS+Android | Lowest cross-platform cost, one team | Similar to RN cost profile | Lowest, but fails offline/NFR-033 requirements |
| Hiring pool (7-10yr horizon) | Large but split (iOS specialists + Android specialists) | **Very large — overlaps with the Ch.14 React hiring pool** | Growing but smaller, Dart-specific | Web hiring pool (large) but doesn't solve the underlying capability gap |
| Enterprise MDM/app-store distribution expectations | Full support | Full support | Full support | Weak — many enterprise buyers expect an installable app for device-management policy reasons |
| Final Recommendation | Rejected — cost doesn't justify marginal performance gain for this app profile | **Selected** | Rejected — technically comparable to RN but forgoes the genuine Ch.14 code-sharing benefit for no offsetting advantage | Rejected — fails Fiona's P0 offline requirement, the single most important persona constraint in this AKB |

**Decision:** React Native is selected on its own independent merits (cost, hiring pool,
offline ecosystem maturity, MDM/distribution fit) — and this independent evaluation happens
to confirm, rather than merely inherit, Chapter 14's forward-looking bet. This satisfies
Chapter 14's CTO condition: the conclusion is the same, but it is now independently earned,
not assumed.

## 3. Frontline-First Design Constraints

| Requirement | Mechanism |
|---|---|
| NFR-033 (≤3-tap resume) | App opens directly to an in-progress-content resume screen, not a generic home/dashboard, for the Learner role specifically |
| NFR-003 (video start on degraded connections) | Adaptive-bitrate player (Ch.27) embedded natively, with aggressive local pre-caching of assigned mandatory content when on Wi-Fi |
| Low-end Android support (Ch.4 §3.1) | Explicit minimum-supported-device performance budget, tested against representative low-tier hardware, not just flagship devices |

## Summary
React Native is independently re-evaluated against Native, Flutter, and PWA-only
approaches — winning on cost, hiring-pool overlap with the web frontend, offline ecosystem
maturity, and enterprise distribution fit, with PWA-only explicitly rejected for failing
Frontline Fiona's P0 offline requirement. This satisfies Chapter 14's condition that mobile
technology be evaluated on its own merits, and the independent conclusion happens to
confirm Chapter 14's original bet rather than merely validate it retroactively.

## Open Questions
Minimum-supported-device performance budget (specific hardware tier) — implementation-phase, informed by real tenant device-fleet data once available.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| React Native's native-module bridge occasionally lags platform OS updates (a known ecosystem risk) | Medium | Medium | Standard mitigation is active dependency maintenance, tracked in [Ch. 49 — Maintenance](../part-9-governance-future/49-maintenance.md) |

## Architecture Decisions
**ADR-059: React Native selected via independent technology evaluation, confirming (not merely inheriting) Ch.14's bet** — §2.

## Future Research
Minimum-device performance budget (implementation phase, Ch.4 device-fleet data).

## Cross References
[Ch. 4](../part-1-foundations/04-user-personas.md) §3.1 (Fiona) · [Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-003, 033) · [Ch. 14](../part-2-system-domain-architecture/14-frontend-architecture.md) (CTO condition) · [Ch. 27](../part-5-media-discovery/27-video-streaming.md) · [Ch. 37](37-offline-learning.md) · [Ch. 49](../part-9-governance-future/49-maintenance.md)

## Definition of Done
- [x] Mobile technology independently evaluated per Chapter 14's explicit CTO condition
- [x] Frontline-first design constraints specified against NFR-003/033

## Confidence Level
**High** — the independent evaluation converges with well-established industry practice for enterprise line-of-business apps of this shape (content/forms/offline-heavy, not graphics-intensive).

## 4. Chapter Review

**Red Team:** The independent evaluation (§2), while procedurally distinct from Chapter
14's bet, arrives at the identical conclusion — a skeptic could reasonably ask whether this
chapter genuinely reconsidered Flutter/Native or performed a evaluation shaped to justify
the pre-existing answer, especially since "code-sharing with Ch.14" is itself one of the
scoring criteria, which structurally favors React Native by construction.

**Blue Team:** This is a fair process critique, and worth being transparent about rather
than dismissing: code-sharing-with-the-web-stack is a *legitimate* evaluation criterion on
its own technical/TCO merits (Ch.1 Principle 6), not a criterion invented to favor a
foregone conclusion — Flutter and Native would be scored identically low on that dimension
regardless of which framework Chapter 14 had chosen. The convergence is coherence, not
circularity: a technology stack that shares patterns with itself is a genuine, defensible
advantage. Noted for the record rather than argued away, since the Red Team's skepticism is
a reasonable prior to state explicitly.

**CTO:** ADR-059 **Approved**. The chapter's transparency about the Red Team's structural-
bias concern, and the Blue Team's substantive (not dismissive) response, satisfies the
spirit of Chapter 14's independent-evaluation condition.

---
*End of Chapter 36. Proceed to Chapter 37 — Offline Learning.*
