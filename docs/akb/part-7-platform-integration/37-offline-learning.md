# Chapter 37 — Offline Learning

> Part VII — Platform & Integration · [Index](../00-index.md) · Previous: [Ch. 36 — Mobile Strategy](36-mobile-strategy.md) · Next: Ch. 38 — Observability

## 1. Purpose

This chapter closes the AKB's longest-standing Open Question — the offline
`ContentProgressed` conflict-resolution strategy, first flagged in Chapter 5 and re-flagged
in Chapters 8, 14, and 36. It delivers FR-018 (offline download/sync) on top of Chapter
36's React Native selection and Chapter 14's service-worker web foundation.

## 2. Resolving the Conflict-Resolution Strategy (Closes Ch.5 Open Question)

Chapter 5 posed three options: highest-progress-wins, last-write-wins, or explicit merge.

| Option | Evaluation |
|---|---|
| Last-write-wins | Rejected — a learner who progresses further offline on Device A, then briefly opens Device B (which syncs first with stale data), would have Device A's real progress overwritten by Device B's stale state — actively harmful |
| Explicit merge (learner prompted to choose) | Rejected — adds friction directly contradicting NFR-033 (Fiona's low-friction resume target); most learners have no meaningful way to judge which state is "correct" |
| **Highest-progress-wins, Selected** | For linear content (video position, page progress), monotonic progress is a safe proxy for correctness — a learner is never worse off resuming from their furthest point. For assessment submissions specifically, this rule does **not** apply (see §3) |

**Decision:** `ContentProgressed` sync uses **highest-progress-wins** for linear content
consumption tracking. This closes Chapter 5's Open Question with a definite answer and
rationale, rather than leaving three options open through six chapters of cross-references.

## 3. Special Case: Assessment Submissions

Highest-progress-wins does not apply to `AssessmentSubmitted` events (Ch.5 §3.5) — these
are **not mergeable/comparable by "progress"** at all. Offline assessment submission uses
the idempotency-key pattern already established in Chapter 13 §4: each submission attempt
carries a client-generated idempotency key, so a resubmission on reconnect is safely
deduplicated rather than "resolved" by any progress heuristic — reusing an existing
mechanism rather than inventing a new one for this special case.

## 4. Offline Architecture

| Layer | Mechanism |
|---|---|
| Content pre-caching | Assigned mandatory content (Ch.25) is proactively downloaded when on Wi-Fi (Ch.36 §3), using React Native's local storage ecosystem |
| Sync queue | Progress/completion events queue locally when offline, flushed on reconnect through the same event-ingestion path as online clients — no parallel offline-specific backend API |
| Conflict resolution | Highest-progress-wins (§2) applied server-side at flush time, not client-side, so the server remains the single arbiter of truth consistent with Chapter 12's event-sourced compliance tier |
| Web PWA parity | Chapter 14's service-worker cache shell provides equivalent (though more limited, per Ch.36 §2's PWA-offline caveat) offline support for the web surface, primarily for Knowledge-Worker Ken's occasional-connectivity scenarios rather than Fiona's primary use case |

## Summary
Highest-progress-wins is selected as the definitive `ContentProgressed` conflict-resolution
strategy, closing an Open Question that has traveled through six prior chapters, with
assessment submissions explicitly carved out as a non-applicable special case handled by
Chapter 13's existing idempotency-key mechanism instead. Offline architecture proactively
pre-caches assigned content on Wi-Fi and resolves sync conflicts server-side at flush time,
keeping the event-sourced backend the single arbiter of truth.

## Open Questions
None remaining from the original Chapter 5 framing — this chapter is a closing chapter for that Open Question.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Highest-progress-wins could mask a legitimate "start over" learner intent (e.g., deliberately restarting a course) as if it were sync conflict | Low-Medium | Low | Explicit "restart" is a distinct user action generating a new `Enrollment` cycle, not a `ContentProgressed` sync event — no actual conflict with this chapter's rule |
| Local pre-cached content storage consumption on low-end devices (Ch.36 §3) | Medium | Medium | Configurable cache-size limits and content-priority (mandatory content prioritized over optional), implementation-phase tuning |

## Architecture Decisions
**ADR-060: Highest-progress-wins for offline `ContentProgressed` conflict resolution; assessment submissions handled separately via existing idempotency-key mechanism** — §2–3, closes Ch.5's Open Question.

## Future Research
Cache-size/priority tuning (implementation phase).

## Cross References
[Ch. 5](../part-1-foundations/05-learning-lifecycle.md) (original Open Question) · [Ch. 8](../part-1-foundations/08-benchmark-analysis.md), [Ch. 14](../part-2-system-domain-architecture/14-frontend-architecture.md), [Ch. 36](36-mobile-strategy.md) (prior cross-references) · [Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-018) · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §5 · [Ch. 13](../part-2-system-domain-architecture/13-api-strategy.md) §4

## Definition of Done
- [x] Conflict-resolution strategy definitively selected and justified, closing the Ch.5 Open Question
- [x] Assessment-submission special case explicitly reconciled with existing Ch.13 mechanism
- [x] Offline architecture specified end-to-end (pre-cache, queue, server-side resolution)

## Confidence Level
**High** — the highest-progress-wins rule is a well-established, low-risk pattern for this exact problem shape, and the assessment special-case correctly reuses rather than duplicates existing infrastructure.

## 5. Chapter Review

**Red Team:** "Highest-progress-wins" for video content specifically is ambiguous — is
progress measured by furthest-timestamp-ever-reached, or by completion-percentage, and what
happens if a learner deliberately rewinds to re-watch a section (legitimate regression in
raw timestamp, not a sync conflict)?

**Blue Team:** Accepted — valid refinement needed. Addendum: "progress" for video content
specifically means **furthest-timestamp-ever-reached (a monotonic high-water mark)**, not
current playback position — a deliberate rewind-to-review doesn't decrease the stored
high-water mark, so it's never mistaken for a sync conflict. This refinement is now part of
ADR-060's binding definition, not left ambiguous.

**CTO:** ADR-060 **Approved with Conditions** — condition is the Blue Team's
high-water-mark refinement for video-content progress is binding and must be implemented
precisely as specified, not left to individual interpretation.

---
*End of Chapter 37. Proceed to Chapter 38 — Observability.*
