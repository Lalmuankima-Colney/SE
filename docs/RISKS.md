# Risk Register – PIEMS

Put this file at `docs/RISKS.md`. Review at every sprint review and record the date in the review log.

**Scoring.** Probability (P) and impact (I) are scored 1 to 5 (1 very low, 5 very high). Exposure = P × I. Treat exposure of 12 or more as high: it needs an owner, a named trigger and a check at every sprint review. Scores below are initial estimates; change them with a reason.

## Register

| ID | Risk | P | I | Exposure | Mitigation | Contingency | Early-warning trigger | Owner | Status |
|---|---|---|---|---|---|---|---|---|---|
| R1 | Scope creep | 4 | 4 | 16 | MoSCoW tiers; Must items frozen in week 2; changes only through change requests | Drop Could items | More than one new Must-level request in a sprint | Project Lead | Open |
| R2 | Floating-point money errors | 1 | 5 | 5 | `Money` value object from day 1; rounding tests | Fix inside the one class | Any test that compares money with a tolerance | Domain Lead | Mitigated |
| R3 | Team member unavailable | 3 | 4 | 12 | Pair on core classes; shared board; documented design | Reassign Should items | A member misses two check-ins in a row | Project Lead | Open |
| R4 | Integration left too late | 3 | 4 | 12 | Walking skeleton in week 2; CI on every push | Freeze features early | Branches open for more than a week | CI / Integration Lead | Open |
| R5 | Data loss or corruption | 2 | 4 | 8 | Database transactions; atomic two-leg transfers (D-23); backup before migration | Restore the backup | A failed migration or an unreadable file | Persistence Lead | Managed |
| R6 | Persistence harder than planned | 1 | 3 | 3 | Week 1 spike; in-memory repositories for early work | Simplify the schema | Spike not finished in week 1 | Persistence Lead | Mitigated |
| R7 | Ambiguous or changing requirements | 3 | 4 | 12 | Acceptance criteria per requirement; change-control issue; Gate A1 resolutions | Re-baseline the SRS | A requirement with no testable criterion | Requirements Analyst | Open |
| R8 | Test gaps | 4 | 3 | 12 | Coverage gate; traceability matrix; review checklist; concrete test IDs | Add tests before release | A requirement with no test ID at sprint end | QA / Test Lead | Open |
| R9 | Code nobody can explain | 2 | 4 | 8 | Everyone walks through every core class before the viva; follow the course's AI-use rules and disclose tool use if required | Rewrite in own words | A member cannot explain a class they own | – | Open |
| R10 | Tooling or environment problems | 3 | 3 | 9 | Maven wrapper; pinned versions; offline-capable build; multi-OS test script | Use a lab machine | Build works on one machine only | CI / Integration Lead | Open |
| R11 | Budget alert flapping around threshold | 3 | 3 | 9 | Hysteresis: reset to OK only when spending drops below 80% (D-28) | Suppress repetitive alerts in session | Rapid oscillations around budget threshold | Domain Lead | Open |
| R12 | Recurring rule month-end date overflow | 3 | 3 | 9 | Clamping to end of month in date arithmetic (D-29) | Log clamping notification | Month-end scheduled rules (29th-31st) | Domain Lead | Open |
| R13 | CSV import bad rows or unknown entities | 4 | 2 | 8 | Row-by-row validation, report line error, continue valid rows (D-30) | User prompt for entity mapping | Unmatched account or category in CSV | Service Lead | Open |
| R14 | Category deletion causing orphan transactions | 3 | 4 | 12 | Soft-delete archive flag on categories (D-26) | Block deletion in repository | Delete request on category with transactions | Repository Lead | Open |
| R15 | Two-leg transfer leg desynchronization | 2 | 5 | 10 | Single JDBC transaction with transfer_link_id; atomic edit/delete (D-23) | Periodic reconciliation check | Mismatched debit/credit pair | Repository Lead | Open |
| R16 | Percentage share rounding drift | 3 | 2 | 6 | Largest Remainder Method (Hare-Niemeyer) for 100.0% sum (Q-014) | Adjust largest bucket | Shares summing to 99.9% or 100.1% | Report Lead | Open |

## Review log

| Date | Sprint | Changes (new risks, rescored, closed) | Reviewers |
|---|---|---|---|
| – | – | Initial register | – |
| 2026-11-13 | Sprint 0 (Gate A1) | Rescored R5 (15), R7 (12), R8 (12), R10 (9); added R11 to R16; assigned owners to high exposure risks | Team (Gate A1 audit) |
| 2026-11-13 | Sprint 0 (Step 2.2) | R2 rescored from 15 to 5 (Mitigated via immutable Money value object and TC-NFR01-01 pass) | Team (Step 2.2 audit) |
| 2026-11-13 | Sprint 0 (Step 2.3) | R6 rescored from 9 to 3 (Mitigated via successful Step 2.3 JDBC spike); R5 rescored from 15 to 8 (Managed via proven rollback and foreign keys) | Team (Step 2.3 spike) |
| 2026-11-15 | Sprint 4 (Step 7.1) | R5 note: FR-20 (automated backup/restore) and NFR-10 (migration backup) deferred as Could items; database safety and atomic writes are governed by Decision D-33 (JDBC transaction template helper with autoCommit false and rollback) and D-23 (atomic two-leg transfer persistence) | Team (Sprint 4 review) |

## Closed or realised risks
Move a risk here when it can no longer occur, or when it happened. For a realised risk record what happened, what it cost, and whether the mitigation worked; this is useful material for the final report.

| ID | Outcome | Lesson |
|---|---|---|
| | | |

## Maintenance risks (post-release)

| Risk | Mitigation |
|---|---|
| Newer JDK breaks the build | Pinned Maven plugins; CI on more than one JDK if possible |
| Database schema must change | Versioned migration scripts; backup first (NFR-10) |
| New report or account type needed | Extension points (strategy, subclass); design review of NFR-09 |
