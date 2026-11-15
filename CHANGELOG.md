# Changelog

All notable changes to the PIEMS (Personal Income and Expense Management System) project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-11-15 (Tag v1.0: Final Release)

### Added
- **Budget Persistence & Restart Hysteresis (Phase G, Step 8.3 Part A, Decision D-46):**
  - Contract interface `BudgetRepository` with `InMemoryBudgetRepository` and `JdbcBudgetRepository` implementations.
  - Added unique index `idx_budget_cat_month` on SQLite table `budgets(category_id, month)` in `Schema.java` ensuring category-month uniqueness.
  - Wired `BudgetRepository` into `SqliteDatabase` and `FinanceService`. Enforced full constructor injection in `Main.java` without fallback defaults.
  - Added `FinanceService.preloadBudgets()` to populate `BudgetMonitor` upon application launch and reset session notification state.
  - Fail-first proof `RestartPersistenceCheckTest` (TC-FR18-09) demonstrates budgets survive application restart.
  - Production wiring restart test `MainBudgetRestartTest` (TC-FR18-10) and restart hysteresis re-arming test `BudgetRestartAlertTest` (TC-FR10-16).
- **Project Limitations and Disciplines Applied (Phase G, Step 7.5):**
  - Authored comprehensive report `docs/LIMITATIONS_AND_LESSONS.md` covering 12 project limitations and a two-column evidence comparison between findings discovered in this project (F-401, F-402, F-104, F-101, F-06..08, LIKE wildcards, CSV injection) and design risks avoided.
  - Linked from `docs/COVERAGE.md` Unit 1 and `README.md`.
- **Viva Voce Examination Preparation Sheets (Phase G, Step 7.4):**
  - Created 6 core class viva sheets (`audit/viva/` for `money.md`, `account.md`, `transaction.md`, `budget.md`, `report-strategy.md`, `finance-service.md`) and navigation index `index.md` with 10 technical defense questions each, citing verified metrics and source locations.
  - Risk R9 mitigated pending viva rehearsal by each team member.
- **Build and Run Guide (Appendix B in README.md):**
  - Added comprehensive platform-specific build and run instructions for Windows (PowerShell) and macOS (Terminal), covering test execution, Swing GUI launch, interactive console UI, custom SQLite database paths, executable JAR execution, and JaCoCo coverage reports.

### Changed
- **Documentation Status Synchronization (Step 8.3 /sync-docs, Decision D-47):**
  - Aligned statuses across `docs/SRS.md`, `docs/COVERAGE.md`, and `docs/TEST_PLAN.md`: FR-11 (recurring rules) and FR-12 (savings goals) set to "Partially verified: domain only, not reachable from service or UI"; FR-18 set to "Partially verified" (accounts, categories, transactions, and budgets persist across restart; goals and recurring rules deferred under Decision D-47).
  - Main application entry point `Main.java` strictly uses full constructor injection with no silent in-memory fallback.

### Deferred
- **Backup, Restore & Migration (FR-20, NFR-10):** Deferred (Could items, not implemented in v1.0; manual SQLite file copy; step 5.5 not scheduled).
- **Service Reachability for Recurring Rules & Goals (FR-11, FR-12):** Deferred under Decision D-47.

## [0.9.0] - 2026-11-15 (Tag v0.9: Sprint 4 QA Campaign & Hardening)

### Added
- **White-Box Testing & Branch Coverage Expansion (Phase F, Step 6.2, Decision D-41):**
  - Expanded branch coverage for `Budget`, `BudgetMonitor`, `CsvImporter`, `Expense`, and `Income` via white-box test suites: `BudgetEvaluateBranchTest`, `BudgetMonitorBranchTest`, `CsvImporterBranchTest`, `ExpenseNoOpBranchTest`, and `IncomeNoOpBranchTest`.
  - Recorded Decision D-41 defining `applyTo` and `reverseOn` defensive no-op behavior on mismatched accounts. Bytecode mutation testing (PITest) documented as constrained by JVM class version 69 in `audit/2026-11-15-campaign/final-report.md`.
- **Black-Box Acceptance Testing Suite (Phase F, Step 6.3, Decision D-42):**
  - 32 new black-box acceptance tests derived from external specifications before code inspection:
    - IEEE 829 debit decision table R1–R5: `DebitDecisionTableBlackBoxTest` (5 tests).
    - UC-03 expense entry main and alternative flows (4a–7a): `ExpenseUseCaseFlowBlackBoxTest` (5 tests).
    - State transition models for `SavingsGoal` and `Budget`: `StateTransitionBlackBoxTest` (5 tests).
    - Requirements acceptance matrix across FR-01 through FR-19: `RequirementAcceptanceBlackBoxTest` (17 tests).
  - Clarified specification ambiguities F-06 (UC-03 Alt 6a `PersistenceException`), F-07 (FR-12 goal over-contribution), and F-08 (FR-10 budget hysteresis silent re-arming) in `audit/2026-11-15-blackbox/findings.md` and updated `docs/SRS.md` (v0.4).
- **System Scenarios & Performance Benchmarking (Phase F, Step 6.4, Decision D-43):**
  - Automated system scenarios S1 through S6 in `SystemScenarioTest` with temporary SQLite databases:
    - S1: Hand-calculated month verification (net savings 1,350.00, savings rate 45.0%, checking 2,450.00, wallet 0.00).
    - S2: Budget alert threshold crossing (WARNING at 84%, EXCEEDED at 102%, zero duplicate alerts).
    - S3: Transfer failure atomicity rollback (zero partial balance change or orphan rows).
    - S4: Recurring transactions catch-up (4 transactions for 3 months downtime, idempotent second run).
    - S5: 10,000 transaction performance benchmark meeting NFR-02 latency targets (listing median 38 ms < 1,000 ms target; monthly report median 26 ms < 2,000 ms target).
    - S6: Clean restart persistence check (identical records and balances across connection lifecycles) and graceful failure on corrupt database files.
  - S6 backup/restore and schema migration checks: Deferred (Could items FR-20 and NFR-10 not implemented; manual SQLite database file copy; step 5.5 not scheduled).
- **QA Hardening Proposals Implemented (Phase F, Step 6.8, Decision D-44):**
  - P-301: Architectural network isolation check added to `scripts/CheckPackages.java` forbidding `java.net.*` and `javax.net.*` imports in `src/main`, verified via `TC-NFR05-02` in `PackageDependencyTest`.
  - P-303: Escaped SQL LIKE wildcards (`%`, `_`, `\`) with explicit `ESCAPE '\'` in `JdbcTransactionRepository`, verified via contract test `TC-FR08-04` in `TransactionRepositoryContractTest`.
  - P-304: Neutralized spreadsheet formula injection (`=`, `+`, `-`, `@`, `\t`, `\r`, `'`) restricted to free-text columns in `CsvExporter`, with lossless unescaping in `CsvImporter`, verified via round-trip test `TC-FR16-06` in `CsvRoundTripTest`.
  - P-308: CLI database connection URL auto-prepends `jdbc:sqlite:` for raw file paths or names in `Main.java`, verified via `TC-FR18-07` in `MainTest`.
  - P-309: Standardized acceptance trial seed script `scripts/seed-trial.sql` without stored balance column (complying with D-25 derived balance architecture), verified via `TC-FR18-08` in `SystemScenarioTest`.
  - P-305: Documented as deferred (overflow unreachable under 999M limit; `ArithmeticException` outside D-13 checked exception policy).
- **Quality Assurance & Trial Preparation (Phase F, Steps 6.5 & 6.7):**
  - Published consolidated QA campaign report `audit/2026-11-15-campaign/final-report.md`.
  - Created standardized acceptance trial protocol and scoring sheet `audit/acceptance-checklist.md`; validation trial pending.

## [0.3.0] - 2026-11-15 (Tag v0.3: Sprint 3 Reports, Exporters, Search, Gate D & Swing GUI)

### Added
- **Financial Reporting Engine (`piems.report` & `piems.service`):**
  - Strategy pattern architecture (`ReportStrategy.java`, `ReportRequest.java`, `ReportResult.java`) enabling arbitrary report addition with zero modifications to `FinanceService` (Open/Closed Principle, NFR-09, `TC-NFR09-01`).
  - Monthly summary report (`MonthlySummaryReport.java`, FR-13) computing income, expenses, net savings, and handling zero income with "N/A" (Decision D-36, Q-013, `TC-FR13-01`, `TC-FR13-02`).
  - Category breakdown report (`CategoryBreakdownReport.java`, FR-14) guaranteeing percentage shares sum to exactly 100.0% using the Hare-Niemeyer Largest Remainder Method (Decision D-36, Q-014, `TC-FR14-01`, `TC-FR14-02`).
  - Multi-month cash-flow trend report (`CashFlowReport.java`, FR-15) generating contiguous monthly rows with zero-filled gaps (`TC-FR15-01`).
- **Data Exchange & Exporters (`piems.report` & `piems.service`):**
  - Template Method export framework (`Exporter.java`) with CSV (`CsvExporter.java`) and aligned tabular plain-text (`TextExporter.java`) implementations (Decision D-10, D-37).
  - RFC 4180 compliant CSV statement importer (`CsvImporter.java`, FR-17) featuring state-machine tokenization, multiset signature reconciliation against existing ledger records, and 1-based line error reporting (`TC-FR17-01`, `TC-FR17-02`).
  - Lossless round-trip export/import into empty ledgers with two-leg transfer deduplication into 7-column interchange format (`TC-FR16-01`, `TC-FR16-02`, Decision D-30, D-37, SRS 3.3).
- **Search & Parameterized Filtering (`piems.repository` & `piems.service`):**
  - Immutable specification query record `TransactionQuery.java` with fluent builder and in-memory `matches` predicate.
  - Parameterized SQL filtering combined with logical `AND` across date ranges, accounts, categories, and case-insensitive text search in both in-memory and SQLite JDBC repositories (Decision D-38, FR-08, `TC-FR08-01`).
- **Atomic Two-Leg Transfer Deletion & Gate D Certification (`piems.repository` & `piems.service`):**
  - Added `findByTransferLinkId` and `deleteByTransferLinkId` across repository contracts and implementations.
  - Added `deleteTransaction` and `editTransaction` to `FinanceService` facade, atomically deleting/editing both transfer legs and restoring derived balances (FR-06, Decision D-23, D-39, `TC-FR06-01`, `TC-FR06-02`).
  - **GATE D Officially Certified:** All 13 Must functional requirements (FR-01 to FR-07, FR-09, FR-10, FR-13, FR-14, FR-18, FR-19) verified with passing automated tests.
- **Desktop Swing Graphical User Interface (`piems.ui.swing`):**
  - Model-View-Presenter (MVP) architecture (`MainView.java`, `MainPresenter.java`) strictly decoupled from Swing widgets and tested headlessly without window initialization (Decision D-15, D-40).
  - Sortable transaction table backed by `TransactionTableModel.java` with `TableRowSorter`.
  - Accessible budget status evaluation badge (`BudgetStatusBadge.java`) providing dual visual indicators using colour plus explicit text descriptors (`[OK]`, `[WARNING]`, `[EXCEEDED]`).
  - Top-level uncaught exception handling (`SwingApp.java`) and dual CLI/GUI launcher (`Main.java --gui`).
  - 9 headless presenter tests verifying expense entry, validation errors, filtering, and budget status badge transitions (`TC-FR07-03`, `TC-FR03-05`, `TC-FR03-06`, `TC-FR19-03`, `TC-FR08-02`, `TC-FR10-07`, `TC-FR10-08`, `TC-FR10-09`, `TC-NFR04-02`).

## [0.2.0] - 2026-11-14 (Tag v0.2: Sprint 2 Budgets, Alerts, Recurring & Goals)

### Added
- **Budgeting Subsystem (`piems.domain` & `piems.service`):**
  - Monthly spending limit entity `Budget.java` with enum `BudgetStatus.java` (`OK`, `WARNING`, `EXCEEDED`).
  - Basis-path white-box verified threshold evaluation (80.00% warning, 100.00% exceeded) strictly using integer minor-unit arithmetic without floating-point conversions (`TC-FR10-01` through `TC-FR10-05`).
  - Observer pattern notification engine (`BudgetListener.java`, `BudgetAlertEvent.java`, `BudgetMonitor.java`) integrated into `FinanceService.java`.
  - Hysteresis threshold state reset per Decision D-28 (re-arming alerts only after dropping below 80% to eliminate threshold flapping).
  - Observer exception containment per Decision D-09 (malfunctioning listeners log/swallow errors and never abort transaction persistence).
  - Full system integration verification for Scenario S2 in `BudgetAlertScenarioTest.java`.
- **Recurring Transactions Subsystem (`piems.domain` & `piems.service`):**
  - Configurable transaction automation `RecurringRule.java` and `Schedule.java` supporting `DAILY`, `WEEKLY`, and `MONTHLY` intervals.
  - Injected `java.time.Clock` architecture eliminating static `LocalDate.now()` calls in business layers.
  - Month-end date clamping per Decision D-29 (rules scheduled on the 31st clamp to the 28th/29th in February and 30th in 30-day months, `TC-FR11-03`).
  - Offline downtime catch-up generator `RecurringGenerator.java` generating one transaction per due date without duplicates (`TC-FR11-01`, `TC-FR11-02`, Scenario S4).
- **Savings Goals Subsystem (`piems.domain`):**
  - Target tracking entity `SavingsGoal.java`, `GoalStatus.java` (`ACTIVE`, `ACHIEVED`), and `GoalContribution.java` with full lifecycle transition table matching `docs/diagrams/state-goal.mmd`.
  - Goal scope bounded per Decision D-35 (Q-008: `OVERDUE` and `CANCELLED` states deferred post-v1.0).
  - Progress tracking with boundary verification at 0%, 99.99%, 100%, and >100% (`TC-FR12-01`).
- **Audit & Verification Baseline:**
  - Read-only audit report `audit/2026-11-14-must/final-report.md` verifying 9 of 13 Must functional requirements, logging findings F-01 to F-05 and remediation proposals P-101 to P-105.
  - Test suite expanded from 183 to 229 automated tests (100% passing).

## [0.1.0] - 2026-11-14 (Tag v0.1: Sprint 1 Core Domain & Persistence)

### Added
- **Core Domain Model (`piems.domain`):**
  - Immutable `Money` value object storing non-negative minor units (Decision D-06, D-24, D-31).
  - Polymorphic `Account` hierarchy (`CashAccount`, `BankAccount`, `CreditCardAccount`) with zero overdraft for bank accounts (Decision D-27) and dedicated liability debt tracking for credit cards (Decision D-31).
  - Polymorphic `Transaction` hierarchy (`Income`, `Expense`, `Transfer`) with double-entry two-leg transfer representation (Decision D-23) and self-balancing invariant.
  - `TransactionFactory` enforcing category and account validation invariants without `instanceof` inspection.
  - Composite `CategoryComponent` hierarchy (`Category`, `CategoryGroup`) supporting recursive leaf traversal and soft-delete archiving (Decision D-26).
  - Checked domain exception hierarchy (`PiemsException`, `ValidationException`, `InsufficientFundsException`, `RecordNotFoundException`, `PersistenceException`) following Decision D-13.
- **Persistence Architecture (`piems.repository`):**
  - Repository interfaces (`AccountRepository`, `CategoryRepository`, `TransactionRepository`) with full dual implementations: in-memory (`InMemory*`) and SQLite JDBC (`Jdbc*`).
  - Automated SQLite schema initialization (`Schema.java`) with connection-level foreign key PRAGMA enforcement (Decision D-32).
  - Transaction boundary management with automatic rollback on error (Decision D-33).
  - Derived balance computation ensuring no persistent balance columns exist (Decision D-25, Q-004).
- **Service & Facade Layer (`piems.service`):**
  - `FinanceService` facade providing clean orchestration for UC-01 through UC-05, net worth calculation (FR-07), and derived balance queries.
  - `TransactionView` decoupled DTO record protecting presentation components from domain and repository internals.
- **Console User Interface (`piems.ui` & `piems.app`):**
  - Interactive console application (`ConsoleApp`, `ConsoleMenu`, `Main`) adhering to $\le 4$ inputs for expense entry (NFR-04, TC-NFR04-01).
  - Balance feedback printed after every financial transaction.
  - Strict containment of `System.out` to `piems.ui` and `piems.app`.
- **Quality Gates & Test Infrastructure:**
  - 183 automated JUnit 5 tests covering contract suites, equivalence partitions, boundary values, fault-injection rollback atomicity, and package dependency rules.
  - Automated documentation consistency test `DocConsistencyTest` enforcing zero drift across requirements, decisions, iteration logs, and Surefire execution counts (Decision D-21, D-34).
  - Architectural verification script (`scripts/CheckPackages.java`, `scripts/check-packages.cmd`) verifying 0 package cycles and strict domain isolation ($I=0.000$).
  - Full Maven build verification with 0 Checkstyle violations and JaCoCo coverage (Domain: 90.4% line, 80.6% branch; Overall: 82.0% line, 71.5% branch).
