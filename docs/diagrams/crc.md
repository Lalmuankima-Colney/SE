# Class-Responsibility-Collaborator (CRC) Cards – PIEMS

**Date:** 2026-11-13  
**Artifact:** `docs/diagrams/crc.md`  
**Sources:** `docs/PROJECT_DESCRIPTION.md` §8, `docs/SRS.md`, `docs/DECISIONS.md` (D-05 to D-15, D-23 to D-31).

---

## 1. Class: `Money`
- **Package:** `piems.domain`
- **Stereotype:** Value Object (Immutable)

| Responsibilities | Collaborators |
|---|---|
| Encapsulate non-negative monetary amounts strictly as 64-bit integer minor units (`minorUnits >= 0`, Decision D-06, D-31). | `java.util.Currency` |
| Perform exact integer arithmetic operations (`add`, `subtract`, `negate`) with overflow checks and zero rounding drift (NFR-01). | |
| Provide value equality, hash code, and natural ordering (`Comparable<Money>`). | |
| Parse decimal strings with at most 2 decimal digits (`of(String)`), rejecting invalid or negative inputs with `IllegalArgumentException`. | |

- **Cohesion Note:** High functional cohesion; encapsulates purely monetary value representation, exact minor-unit arithmetic, and scale invariants.
- **Coupling Note:** Low coupling ($Ce = 0$ internal, $Ca = \text{high}$); completely self-contained within `domain` without depending on any other PIEMS package.
- **Flagged Misplaced Responsibilities:**
  - *Localized UI Display Formatting:* Prepending currency symbols (e.g. `₹`) or generating localized presentation strings belongs to the presentation layer (`ui`), not `Money`.
  - *Debt / Negative Balances:* Storing negative values directly in `Money` was rejected (Decision D-31); credit card debts and liability balances must be modeled explicitly as separate variables.

---

## 2. Class: `Account` (Abstract)
- **Package:** `piems.domain`
- **Stereotype:** Entity (Polymorphic Hierarchy: `CashAccount`, `BankAccount`, `CreditCardAccount`)

| Responsibilities | Collaborators |
|---|---|
| Maintain account identity, unique human-readable name, opening balance (`Money`), and archived status flag (Decision D-26). | `Money` |
| Provide abstract debit evaluation rule `canDebit(Money)` implemented polymorphically per account type (R1–R5 debit decision table; cash/bank reject overdraft per D-27, card validates credit limit). | `Transaction` |
| Derive current balance dynamically from opening balance plus the sum of applied transaction effects (`opening_minor + sum(effects)`, Decision D-25). | |
| Enforce account archival invariant: archived accounts keep complete ledger history but reject all new debits. | |

- **Cohesion Note:** High domain cohesion; models core account identity, debit validation policies, and balance derivation.
- **Coupling Note:** Low coupling ($Ce = 2$ [`Money`, `Transaction`], $Ca = \text{moderate}$); pure domain abstraction isolated from persistence and UI.
- **Flagged Misplaced Responsibilities:**
  - *Persistent Balance Storage:* Maintaining a mutable `balance` column in SQL was flagged and rejected (Q-004, D-25); balance is derived.
  - *Direct SQL Execution:* Querying and persisting account records belongs to `AccountRepository`, never embedded in `Account` methods (D-05, D-11).

---

## 3. Class: `Transaction` (Abstract)
- **Package:** `piems.domain`
- **Stereotype:** Entity (Polymorphic Hierarchy: `Income`, `Expense`, `Transfer`)

| Responsibilities | Collaborators |
|---|---|
| Encapsulate transaction metadata: unique ID, ISO date, positive amount (`Money`), note, associated `Account`, and optional `CategoryComponent`. | `Money` |
| Polymorphically calculate net balance effect on accounts via `effectOn(Account)` (`Income` adds, `Expense` subtracts, `Transfer` subtracts from source and adds to destination). | `Account` |
| For `Transfer`, maintain destination account and shared UUID `transferLinkId` linking the debit and credit legs (Decision D-23). | `CategoryComponent` |

- **Cohesion Note:** High informational and domain cohesion; encapsulates single ledger events and their mathematical effect on account balances.
- **Coupling Note:** Moderate coupling ($Ce = 3$ [`Money`, `Account`, `CategoryComponent`], $Ca = \text{high}$); depended on across service, repository, and reporting layers.
- **Flagged Misplaced Responsibilities:**
  - *Instanceof Chains in Service:* Inspecting transaction subclasses with `instanceof` to calculate balances is prohibited (OOM rule); polymorphism via `effectOn(Account)` must be used.
  - *Persistence Coordination:* Inserting transaction rows into the database belongs to `TransactionRepository`, not `Transaction` domain entities.

---

## 4. Class: `Budget`
- **Package:** `piems.domain`
- **Stereotype:** Entity

| Responsibilities | Collaborators |
|---|---|
| Define a spending limit (`Money`) for a specific `CategoryComponent` and calendar month (`YearMonth`). | `Money` |
| Evaluate spent amount against limit to determine `BudgetStatus` (`OK`, `WARNING`, `EXCEEDED`) using integer minor-unit comparisons without tolerance drift (Decision D-09, D-28). | `CategoryComponent` |
| Enforce non-negative limit invariant (`limit > 0`), throwing `IllegalArgumentException` on invalid construction limits. | `BudgetStatus` |

- **Cohesion Note:** High functional cohesion; encapsulates the business rules for monthly spending thresholds.
- **Coupling Note:** Low coupling ($Ce = 3$ [`Money`, `CategoryComponent`, `BudgetStatus`], $Ca = \text{moderate}$); completely decoupled from alerting side effects.
- **Flagged Misplaced Responsibilities:**
  - *Alert Exceptions:* Throwing exceptions upon threshold crossing is prohibited (Decision D-09); threshold evaluation is normal business flow.
  - *Observer Subscription Management:* Maintaining listener lists and dispatching alert notifications belongs to `BudgetMonitor` (Observer subject), not the `Budget` entity.

---

## 5. Class: `FinanceService`
- **Package:** `piems.service`
- **Stereotype:** Facade & Application Orchestrator

| Responsibilities | Collaborators |
|---|---|
| Serve as the single application entry point (Facade pattern, Decision D-05) for all UI layers (`ConsoleApp`, GUI). | `AccountRepository` |
| Validate user inputs and throw checked `ValidationException` subclasses before modifying domain state (Decision D-13). | `TransactionRepository` |
| Coordinate business use cases (record income/expense, atomic two-leg transfers, category tree management, budget evaluation, recurring rule execution, savings goal contributions). | `CategoryRepository` |
| Enforce persistence transactions and automatic rollback on error across repository interfaces (Decision D-23, D-33). | `BudgetRepository` |
| Calculate total system net worth and derive account balances. | `BudgetMonitor` |
| Forward reporting requests to registered `ReportStrategy` implementations without type checking. | `ReportStrategy` |
| Provide deterministic time using an injected `java.time.Clock` (Decision D-14). | `Clock`, Domain Models |

- **Cohesion Note:** High facade orchestration cohesion; unifies all application use cases while keeping domain and persistence decoupled.
- **Coupling Note:** Moderate efferent coupling ($Ce \approx 8$), low afferent coupling ($Ca \approx 1$ [`ConsoleApp`]); normal for an architectural Facade mediating UI and domain.
- **Flagged Misplaced Responsibilities:**
  - *Presentation Logic:* Reading stdin, printing menus, and formatting terminal output belong strictly in `ui` (`ConsoleApp`, `ConsoleMenu`).
  - *Direct SQL Queries:* Writing SQL or accessing JDBC directly in service methods is strictly forbidden (D-05, D-11); all persistence must use repository interfaces.
  - *Duplicate Debit Calculations:* Checking overdraft or credit limits directly in service methods instead of delegating to `Account.canDebit()`.

---

## 6. Class: `ReportStrategy` (Interface)
- **Package:** `piems.report`
- **Stereotype:** Strategy (Concrete: `MonthlySummaryReport`, `CategoryBreakdownReport`, `CashFlowReport`)

| Responsibilities | Collaborators |
|---|---|
| Define standard reporting generation contract (`generate(ReportRequest): ReportResult`, Decision D-10). | `Transaction` |
| Aggregate transaction history into structured reporting summaries (income, expense, savings rate, category shares, cash-flow trends). | `CategoryComponent` |
| Apply Largest Remainder Method (Hare-Niemeyer, Decision Q-014) to ensure category percentage breakdown shares sum to exactly 100.0%. | `Money` |
| Exclude transfer transaction legs from income/expense calculations (Decision D-23). | `ReportRequest` |

- **Cohesion Note:** High strategic cohesion; each concrete strategy implements a single, focused financial reporting algorithm.
- **Coupling Note:** Low coupling ($Ce = 3$ [`Transaction`, `CategoryComponent`, `Money`], $Ca = \text{low}$); satisfies Open-Closed principle and NFR-09 (modifiability).
- **Flagged Misplaced Responsibilities:**
  - *Export File Rendering:* Generating CSV text or formatted strings belongs to `Exporter` (Template Method pattern, Decision D-10), not `ReportStrategy`.
  - *Direct Database Queries:* Reports must operate on transaction collections supplied via service/repository queries rather than executing their own SQL queries.
