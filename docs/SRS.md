# Software Requirements Specification – Personal Income & Expense Management System (PIEMS)

Put this file at `docs/SRS.md`. Structure follows IEEE 29148. Items marked **TBD** are open and listed in section 6.

| Field | Value |
|---|---|
| Version | 0.5 |
| Date | 2026-11-15 |
| Authors | Team (Domain Lead, Requirements Analyst, UI Lead, Persistence Lead) |
| Status | Approved (Must requirements frozen per D-04; Gate A1 closed) |

## 1. Introduction

### 1.1 Purpose
This document specifies the functional and non-functional requirements of PIEMS, a single-user, offline desktop application for recording income and expenses, setting budgets and producing reports. It is the reference for design, implementation and testing, and for acceptance by the user and the instructor.

### 1.2 Scope
PIEMS manages accounts, transactions (income, expense, transfer), categories, monthly budgets with alerts, recurring transactions, savings goals, reports and CSV import and export. Data is stored locally. Out of scope: multi-user login, bank API synchronisation, multi-currency conversion, cloud sync and a mobile application.

### 1.3 Definitions

| Term | Meaning |
|---|---|
| Account | A place where money is held: cash, bank or credit card |
| Transaction | A recorded movement of money: income, expense or transfer |
| Transfer | A movement between two of the user's accounts; net worth does not change |
| Category | A label for transactions (for example Food, with subcategories) |
| Budget | A spending limit for one category in one month |
| Net worth | The sum of all account balances |
| Minor unit | The smallest currency unit (cent, paisa) used to store money exactly |
| Recurring rule | A schedule that generates transactions automatically |

### 1.4 References
`docs/DECISIONS.md`, `docs/COVERAGE.md`, `docs/TEST_PLAN.md`, `docs/RISKS.md`, the course syllabus and project brief.

## 2. Overall description

### 2.1 Product perspective
A standalone Java application. It replaces the user's spreadsheet or paper ledger (the legacy system) and imports its history through CSV. It has no external network interfaces.

### 2.2 Product functions (summary)
Manage accounts and categories; record, edit and delete transactions; set budgets and receive alerts; schedule recurring transactions; track savings goals; view balances and reports; export and import CSV; back up and restore data.

### 2.3 User classes

| User class | Description | Technical skill |
|---|---|---|
| Primary user | A person tracking personal finances | Basic computer use |
| Scheduler (system actor) | Time-triggered generation of recurring transactions | n/a |

### 2.4 Operating environment
Windows, Linux or macOS with JDK 17 or later. Local file storage. No network.

### 2.5 Constraints and assumptions
One currency per ledger. Default currency is Indian Rupee (code `INR`, symbol `₹`), with 2 decimal places (minor unit factor = 100), stored as a configurable constant in `AppConfig` (D-24). Single user and single process, so no concurrent writers. Money stored as integer minor units inside the immutable `Money` value object; `Money` stores non-negative values (`minorUnits >= 0`), with debt and liabilities tracked as separate variables (D-06, D-31, Q-003), while transactions require amount > 0. Java and SQLite are mandated by the project decisions (D-02).

## 3. Specific requirements

### 3.1 Functional requirements
Priorities use MoSCoW. Status values: Proposed, Approved, Implemented, Verified, Deferred.

| ID | The system shall | Priority | Acceptance criterion | Status |
|---|---|---|---|---|
| FR-01 | Allow the user to create, edit and archive accounts of type cash, bank or credit card with an opening balance | Must | An archived account keeps its history and rejects new transactions | Approved |
| FR-02 | Record income | Must | Balance rises by the amount; an amount of zero or less is rejected | Approved |
| FR-03 | Record expense | Must | Balance falls by the amount; bank and cash accounts prohibit overdraft (rejection with InsufficientFundsException); credit card balances may fall below zero up to credit_limit_minor (D-27) | Approved |
| FR-04 | Transfer money between two accounts | Must | Stored as two linked leg rows written atomically inside a single database transaction with a shared transfer_link_id (D-23); net worth is unchanged; edit and delete act on both legs together atomically; excluded from monthly summary (FR-13) and category breakdown (FR-14) | Approved |
| FR-05 | Manage categories and subcategories | Must | A category in use cannot be deleted (reassign or soft-delete via archived flag; archived categories reject new transactions per D-26) | Approved |
| FR-06 | Edit and delete transactions | Must | Balances stay consistent after every edit or delete; transfers edit or delete both legs atomically (D-23) | Approved |
| FR-07 | Show account balances and net worth | Must | Net worth equals the sum of account balances; account balances are dynamically derived from opening balance plus transactions (D-25) | Approved |
| FR-08 | Search and filter transactions by date, category, account and text | Should | Filters combine with AND; text search matches literal characters escaping SQL LIKE wildcards (%, _, \\) (D-11, D-44, P-303) | Proposed |
| FR-09 | Allow a monthly budget per category | Must | At most one budget per category per month | Approved |
| FR-10 | Alert at 80% and 100% of a budget | Must | Each threshold alerts once per crossing; BudgetMonitor resets to OK if spending drops below 80%, allowing re-alerts on subsequent re-crossing (D-28) | Approved |
| FR-11 | Generate recurring transactions (daily, weekly, monthly) | Should | Generated exactly once per due date, including catch-up after downtime; rules scheduled on the 29th–31st clamp to the last valid day in shorter months (D-29) | Proposed |
| FR-12 | Track savings goals with progress | Should | Status changes from Active to Achieved when contributions reach the target (Overdue and Cancelled deferred to post-v1.0 per Q-008, D-35) | Approved |
| FR-13 | Produce a monthly summary (income, expense, savings rate) | Must | Totals match the underlying transactions; transfer legs excluded; savings rate displays "N/A" (or "-") when monthly income is zero (Q-013) | Approved |
| FR-14 | Produce a category breakdown report | Must | Category shares sum to exactly 100.0% using Largest Remainder Method (Hare-Niemeyer); transfer legs excluded (D-23, Q-014) | Approved |
| FR-15 | Produce a multi-month cash-flow trend | Should | One row per month in the range | Proposed |
| FR-16 | Export transactions and reports to CSV | Should | An export can be re-imported without loss; transfers exported as one row (kind=TRANSFER, to_account) (D-23, D-30); formula characters on free-text columns neutralized losslessly (D-44, P-304) | Proposed |
| FR-17 | Import a CSV statement with duplicate detection | Could | Re-importing the same file adds nothing; bad rows or unknown entities skipped and reported with line numbers without aborting valid rows (D-30) | Proposed |
| FR-18 | Persist all data locally and reload it on start | Must | State is identical after a restart | Approved |
| FR-19 | Validate all input and show clear messages | Must | No invalid value reaches the domain model | Approved |
| FR-20 | Back up and restore the database file | Could | Restore reproduces the backed-up state | Deferred |

### 3.2 Non-functional requirements

| ID | Category | The system shall | Measure / verification | Status |
|---|---|---|---|---|
| NFR-01 | Accuracy | Handle money exactly, without floating point | Summing 1,000 entries of 0.10 gives exactly 100.00 | Proposed |
| NFR-02 | Performance | List 10,000 transactions in under 1 s and produce a monthly report in under 2 s | Timed system test | Proposed |
| NFR-03 | Reliability | Leave no partial transaction after a failed write | Database transaction plus fault-injection test on transfer | Proposed |
| NFR-04 | Usability | Let a new user add an expense with at most 4 inputs | Trial with 3 people | Proposed |
| NFR-05 | Privacy | Keep data local and make no network calls | Code review and dependency check | Proposed |
| NFR-06 | Portability | Run on Windows, Linux and macOS with JDK 17 | Build and test on two operating systems if possible | Proposed |
| NFR-07 | Maintainability | Have no package cycles, methods of at most 30 lines, domain line coverage of at least 80%, and domain branch coverage of at least 70% | Metrics script and JaCoCo (Q-012) | Proposed |
| NFR-08 | Testability | Keep domain logic independent of the GUI and the database | Domain tests use in-memory repositories | Proposed |
| NFR-09 | Modifiability | Allow a new report or account type by touching at most 2 existing classes | Design review | Proposed |
| NFR-10 | Recoverability | Make an automatic backup before a schema change | Migration test | Deferred |

### 3.3 External interface requirements

**User interface.** Console menu and Swing GUI (`--gui`). Both call the same service facade (`FinanceService`). The Swing GUI employs the Model-View-Presenter (MVP) architecture (D-15, D-40):
- **Expense form:** account combo, category combo, amount (positive decimal), ISO date `yyyy-MM-dd` (defaults to current date), and optional note.
- **Transaction table:** 6 columns (`Date`, `Kind`, `Account ID`, `Category ID`, `Amount`, `Note`), interactive column header sorting via `TableRowSorter`, and query filters (free-text note search, account filter, category filter).
- **Budget status indicator:** displays current-month category spending status using colour plus text (`[OK]`, `[WARNING]`, `[EXCEEDED]`) per Shneiderman/Nielsen accessibility principles.
- **Top-level error handling:** recoverable validation, balance, and persistence errors display in a modal error dialog without crashing or freezing the UI (D-13, D-40).

**CSV file format.**

| Property | Specification |
|---|---|
| Encoding | UTF-8 |
| Header row | Required: `date,kind,account,to_account,category,amount,note` |
| Date | ISO format `yyyy-MM-dd` |
| Kind | Transaction kind: `INCOME`, `EXPENSE`, or `TRANSFER` |
| Amount | Dot as decimal separator, at most 2 decimals, positive magnitude; kind determines effect |
| Transfer row | Single row with `kind=TRANSFER`, `account` (source), and `to_account` (destination); category is empty; import splits into two linked legs (D-23, D-30) |
| Formula neutralization | Free-text columns (note, account, to_account, category) starting with '=+-@\t\r'' are prefixed with '\'' during export to prevent spreadsheet macro execution (CWE-1236); import reverses neutralization losslessly (D-44, P-304). Date and amount columns are never neutralized. |
| Bad row / unknown entity | Skipped and reported with its line number without aborting valid rows |
| Example | `2026-01-15,EXPENSE,Bank,,Food,250.00,Groceries`<br>`2026-01-16,TRANSFER,Bank,Cash,,500.00,ATM withdrawal` |

**Database.** One local SQLite file with foreign keys enabled. Money stored as integer minor units.

### 3.4 Data requirements (starter data dictionary)

| Table | Columns |
|---|---|
| `accounts` | id, name, type, opening_minor, credit_limit_minor, archived (balance dynamically derived per D-25) |
| `categories` | id, name, parent_id, kind, archived |
| `transactions` | id, kind, date, amount_minor, note, account_id, transfer_link_id, category_id, rule_id |
| `budgets` | id, category_id, month, limit_minor |
| `recurring_rules` | id, template fields, frequency, start, end, last_generated |
| `goals` | id, name, target_minor, deadline, status |
| `goal_contributions` | id, goal_id, date, amount_minor |

Add type, constraint and meaning for each column in the design document.

## 4. Use cases

| ID | Name | Actor | Related requirements |
|---|---|---|---|
| UC-01 | Manage accounts | User | FR-01, FR-07 |
| UC-02 | Record income | User | FR-02, FR-06 |
| UC-03 | Record expense | User | FR-03, FR-06 |
| UC-04 | Transfer | User | FR-04, FR-06 |
| UC-05 | Manage categories | User | FR-05 |
| UC-06 | Set budget | User | FR-09 |
| UC-07 | View alerts | User | FR-10 |
| UC-08 | Manage recurring rules | User, Scheduler | FR-11 |
| UC-09 | Manage goals | User | FR-12 |
| UC-10 | Generate report | User | FR-08, FR-13, FR-14, FR-15 |
| UC-11 | Export / import CSV | User | FR-16, FR-17 |
| UC-12 | Backup / restore | User | FR-20 |

### UC-03 Record expense (detailed)
- **Precondition:** at least one active account and one category exist.
- **Main flow:** (1) The user selects Record expense. (2) The system asks for account, category, amount, date and note. (3) The user enters the values. (4) The system validates them. (5) The system checks the account's debit rule. (6) The system saves the transaction and updates the balance in one database transaction. (7) The system re-evaluates the category budget. (8) The system shows the new balance and any alert.
- **Alternative flows:** 4a invalid input: show the message and return to step 3. 5a debit rule fails: reject with an insufficient-funds message. 6a save fails: roll back and report. 7a threshold crossed: raise a budget alert.
- **Postcondition:** balance, ledger and budget status are consistent.

Detail the other use cases in the same format as they are scheduled (**TBD**).

## 5. Models
Context model (DFD level 0), DFD levels 1 and 2, use-case diagram, class diagram, ER diagram, state diagrams: see `docs/diagrams/`.

## 6. Open issues

| # | Issue | Owner | Needed by | Status |
|---|---|---|---|---|
| 1 | Complete UI field list and console commands | UI / Console Lead | After prototype review | Open |
| 2 | Decide the default currency | Domain Lead | Week 2 | Closed at Gate A1: INR (`₹`), 2 decimals (D-24) |
| 3 | Confirm overdraft rule for bank accounts (none, or a configurable limit) | Domain Lead | Week 2 | Closed at Gate A1: No overdraft permitted in v1.0 (D-27) |
| 4 | Detail use cases UC-01, UC-02, UC-04 to UC-12 | Requirements Analyst | Before each sprint | Open |
| 5 | Clarify UC-03 Alternative 6a exception contract (F-06) | System Architect | Sprint 4 QA | Closed: PersistenceException wrapped at facade (D-13, D-42) |
| 6 | Clarify SavingsGoal behavior when contributions exceed target (F-07) | Domain Lead | Sprint 4 QA | Closed: Goal remains ACHIEVED with progress > 100% (D-35, D-42) |
| 7 | Clarify Budget threshold downward reset notification behavior (F-08) | Domain Lead | Sprint 4 QA | Closed: Silent internal reset to OK to re-arm alerts (D-28, D-42) |

## 7. Requirements validation checklist
Review every requirement against: valid (needed by a stakeholder), consistent (no conflict), complete, realistic, verifiable (a test can be written), unambiguous, traceable (has an ID and appears in `docs/COVERAGE.md`). Record the review date, reviewers and defects found below.

| Date | Reviewers | Defects found | Resolved |
|---|---|---|---|
| 2026-11-13 | Team (Gate A1 inspection) | Open issues 2 and 3 closed; schemas aligned with D-23, D-25, D-26 | Yes |

## 8. Change history

| Version | Date | Change | Change request |
|---|---|---|---|
| 0.1 | – | Initial draft | – |
| 0.2 | 2026-11-13 | Gate A1 decisions incorporated: default currency INR (D-24), bank overdraft prohibited (D-27), two-leg transfer schema (D-23), soft-delete category archive (D-26), 70% domain branch coverage in NFR-07 (Q-012), CSV schema updated (D-30), issues 2 and 3 closed, Must requirements frozen per D-04. | Gate A1 / Inspection 2026-11-13 |
| 0.3 | 2026-11-15 | Step 6.3 Black-box QA findings F-06, F-07, F-08 resolved and closed (D-42); full requirement acceptance test matrix derived. | Sprint 4 QA Step 6.3 |
| 0.4 | 2026-11-15 | Step 6.8 QA hardening: CSV formula trigger neutralization and lossless unescaping (FR-16, D-44, P-304); SQL LIKE wildcard escaping (FR-08, D-44, P-303); automated network isolation check (NFR-05, D-44, P-301); CLI database URL auto-prefix (FR-18, D-44, P-308); trial baseline seed script (P-309). | Sprint 4 QA Step 6.8 |
| 0.5 | 2026-11-15 | FR-20 (Backup/restore) and NFR-10 (Schema migration backup) marked as Deferred (Could items, not implemented in v1.0; manual SQLite file copy; step 5.5 not scheduled). | Sprint 4 Review (Step 7.1) |
