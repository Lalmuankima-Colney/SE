# Diagram Analysis & Documentation Discrepancies (Step 2.5)

**Date:** 2026-11-13  
**Artifacts Generated:**  
- [`docs/diagrams/class-domain.mmd`](file:///c:/Users/muank/Desktop/Sem%203/SE/PROJECT%20SE/docs/diagrams/class-domain.mmd)
- [`docs/diagrams/package.mmd`](file:///c:/Users/muank/Desktop/Sem%203/SE/PROJECT%20SE/docs/diagrams/package.mmd)
- [`docs/diagrams/state-goal.mmd`](file:///c:/Users/muank/Desktop/Sem%203/SE/PROJECT%20SE/docs/diagrams/state-goal.mmd)
- [`docs/diagrams/state-budget.mmd`](file:///c:/Users/muank/Desktop/Sem%203/SE/PROJECT%20SE/docs/diagrams/state-budget.mmd)

---

## 1. Identified Document Ambiguities & Contradictions

### Question 1: SavingsGoal Lifecycle States (`Overdue`, `Cancelled` vs `Active`, `Achieved`)
- **Observation:** `docs/TEST_PLAN.md` §9.2 and preliminary design notes in the project description mentioned `Overdue` and `Cancelled` states for savings goals. However, `docs/SRS.md` FR-12 only defines the lifecycle transition from `Active` to `Achieved`.
- **Resolution in Diagrams:** Per approved decision **Q-008** (Gate A1), `Overdue` and `Cancelled` are deferred to post-v1.0. `docs/diagrams/state-goal.mmd` models only `Active <--> Achieved` with an explicit design note explaining the decision.

### Question 2: Transfer Modeling (Single Row vs Two Linked Legs)
- **Observation:** Architectural decision D-12 originally specified storing a transfer as a single database record containing `to_account_id`. In contrast, Project Description §14.5 and accounting principles required a debit leg and a credit leg.
- **Resolution in Diagrams:** Per Decision **D-23** (superseding D-12), `docs/diagrams/class-domain.mmd` and the database schema model transfers as two linked legs sharing `transferLinkId` (UUID). The external CSV interface in SRS §3.3 retains `to_account` as an interchange format, which is split into two legs upon import (D-30).

### Question 3: Account Balance Persistence (Stored Column vs Dynamically Derived)
- **Observation:** Early text in D-12 stated that transfers are written "with the balance updates", implying a stored balance column in `accounts`. However, `docs/SRS.md` §3.4 and domain invariant §14.4 state that balance equals opening balance plus the sum of transaction effects.
- **Resolution in Diagrams:** Per Decision **D-25** and Q-004, account balance is dynamically derived (`opening_minor + sum(effects)`). `Account.balance()` is an abstract method computed on demand; no persistent `balance` column exists in the schema or class diagrams.

### Question 4: Category Archiving vs Deletion
- **Observation:** `docs/SRS.md` FR-05 required categories with existing transactions to be archived rather than hard-deleted. However, the initial SRS §3.4 starter data dictionary omitted the `archived` column from `categories`.
- **Resolution in Diagrams:** Per Decision **D-26** and Q-005, an `archived: boolean` attribute was added to `CategoryComponent` and the `categories` SQLite table.

### Question 5: Money Value Object Sign Invariant
- **Observation:** `docs/TEST_PLAN.md` TC-FR03-03 mentioned credit cards reaching a negative balance (e.g. `-500.00`).
- **Resolution in Diagrams:** Per Decision **D-31** and Q-003, `Money` is strictly non-negative (`minorUnits >= 0`). Credit card debt and liabilities are tracked as distinct non-negative quantities, preventing negative values inside `Money`.

### Question 6: Budget Alert Hysteresis on Spending Reduction
- **Observation:** SRS FR-10 states that alerts trigger upon crossing 80% and 100% thresholds, but did not specify what occurs when an expense is edited or deleted and spending later re-crosses the threshold.
- **Resolution in Diagrams:** Per Decision **D-28**, `docs/diagrams/state-budget.mmd` incorporates hysteresis: spending falling below 80% resets the alert state to `OK`, allowing re-notification if spending rises again.

### Question 7: Missing `docs/PROJECT_DESCRIPTION.md` File
- **Observation:** Prompts and references cite `docs/PROJECT_DESCRIPTION.md` §8.2. As recorded in audit finding `F-111`, this file was an external syllabus document not uploaded to the repository workspace.
- **Resolution in Diagrams:** Class attributes, relationships, and design constraints were constructed strictly from verified sources of truth (`docs/SRS.md`, `docs/DECISIONS.md`, `antigravity-piems/.agents/skills/`) without improvising unapproved concepts.
