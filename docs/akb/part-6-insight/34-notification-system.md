# Chapter 34 — Notification System

> Part VI — Insight · [Index](../00-index.md) · Previous: [Ch. 33 — Analytics](33-analytics.md) · Next: Part VII, Ch. 35 — Integration Architecture

## 1. Purpose

Notification is a Generic subdomain (Ch.10 §3). This chapter closes Part VI by delivering
FR-035 (deadline/overdue notifications across email, in-app, and mobile push).

## 2. Technology Evaluation

| Dimension | Build custom multi-channel delivery (SMTP/push infra) | **Integrate a notification orchestration service (Courier/SendGrid+push-provider-class), Selected** |
|---|---|---|
| Fit to Ch.1 §2.2 (Generic, integrate) | Contradicts the boundary — email/push deliverability is a solved, reputation-sensitive problem | **Consistent** |
| Deliverability reputation management (a genuinely hard, ongoing operational problem) | High burden — sender reputation, bounce handling, spam-compliance all owned in-house | Vendor's core competency |
| Cost (Ch.1 Principle 6) | High operational burden | Usage-based | 
| Final Recommendation | Rejected | **Selected** |

**Decision:** Integrate a multi-channel notification orchestration vendor for actual
delivery (email/SMS/push transport), with the Notification context (Ch.11 #15) owning
templating, tenant-configurable channel preferences, and — critically — **consuming the
domain events already defined in Chapter 5 §5** (`DeadlineApproachingNotified`,
`OverdueEscalated`) as its trigger source, so notification logic is purely reactive to
already-modeled lifecycle events, not a parallel scheduling system.

## 3. Delivery Guarantees & Tenant Preferences

| Aspect | Approach |
|---|---|
| Channel preference | Learner/tenant-configurable (email, in-app, push) with in-app as the guaranteed fallback (never dependent on external deliverability) |
| Delivery tracking | `DeliveryLog` aggregate (Ch.11 #15) records attempt/success/failure per channel, feeding [Ch. 38 — Observability](../part-8-operations/38-observability.md) |
| Grader-queue backlog alerts (Ch.23 Risk) | Reuses this same infrastructure — an internal-facing notification type, not a special case |

## Summary
A multi-channel notification orchestration vendor is integrated for delivery transport,
consistent with Chapter 1's Generic-subdomain boundary, while the Notification context
owns templating and tenant channel preferences and reacts purely to Chapter 5's
already-modeled lifecycle events — avoiding a parallel scheduling system. In-app
notification is the guaranteed fallback channel, and delivery tracking directly serves
both learner-facing reminders and the internal grader-queue-backlog alert Chapter 23
flagged.

## Open Questions
Vendor selection deferred to Ch.46 Licensing.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Notification volume at BR-011 peak concurrency (synchronized deadline reminders) could trigger vendor rate limits | Medium | Medium | [Ch. 43 — Scalability](../part-8-operations/43-scalability.md) should model notification burst volume alongside other BR-011-driven capacity planning |

## Architecture Decisions
**ADR-056: Integrate a notification orchestration vendor for delivery; Notification context owns templating/preferences and reacts to existing Ch.5 domain events, not a parallel scheduler** — §2.

## Future Research
Vendor selection (Ch.46); burst-volume modeling (Ch.43).

## Cross References
[Ch. 1](../part-1-foundations/01-enterprise-lms-overview.md) §2.2 · [Ch. 5](../part-1-foundations/05-learning-lifecycle.md) §5 · [Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-035) · [Ch. 11](../part-2-system-domain-architecture/11-bounded-contexts.md) #15 · [Ch. 23](../part-4-learning-domain/23-assessment-engine.md) · [Ch. 38](../part-8-operations/38-observability.md) · [Ch. 43](../part-8-operations/43-scalability.md)

## Definition of Done
- [x] Delivery technology selected via Technology Evaluation Template
- [x] Reactive-to-existing-events design confirmed, avoiding a parallel scheduling system
- [x] In-app guaranteed-fallback channel and delivery tracking specified

## Confidence Level
**High** — Generic-subdomain buy decision consistent with every prior such decision in this AKB; reactive-to-existing-events design is low-risk since it reuses Chapter 5's already-validated event model.

## 4. Chapter Review

**Red Team:** No mention of notification-fatigue/frequency-capping — a learner with
multiple overdue mandatory trainings could receive a barrage of reminders across three
channels simultaneously, degrading UX and potentially the perceived seriousness of
genuinely urgent compliance deadlines.

**Blue Team:** Accepted — real UX/product concern. Addendum: tenant-configurable
frequency-capping/digest rules (e.g., "at most one digest email per day, urgent
in-app always shown") are added as a required capability of the Notification context, not
a nice-to-have left to the vendor's default behavior.

**CTO:** ADR-056 **Approved with Conditions** — condition is frequency-capping/digest
capability, per the Blue Team addendum, is a binding requirement on this context's design.

---
*End of Chapter 34. This closes Part VI — Insight. Proceed to Part VII — Platform &
Integration, beginning with Chapter 35 — Integration Architecture.*
