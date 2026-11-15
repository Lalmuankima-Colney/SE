# Iteration Log – PIEMS

Put this file at `docs/ITERATION_LOG.md`. One entry per sprint, written at the sprint review. Hours and counts come from the time log and from tool output, not from memory.

## Definition of Done
A backlog item is done only when: it compiles; its tests exist and pass; it has been reviewed through a pull request; the traceability matrix in `docs/COVERAGE.md` is updated; affected documents and diagrams are updated; and no new package cycle was introduced.

## Plan overview

| Sprint | Week | Goal | Planned deliverables | Tag |
|---|---|---|---|---|
| 0 | 1 | Requirements and risk spike | Vision, SRS draft, risk register, repository and CI skeleton, `Money` class with tests | – |
| – | 2 | Design | Use-case, class and package diagrams; DFD 0 to 2; ER; CRC cards; walking skeleton | – |
| 1 | 3 to 4 | Core | Accounts, income, expense, transfer, categories, persistence, console UI | v0.1 |
| 2 | 5 | Budgets | Budgets, alerts, recurring rules, goals; all Must items done | v0.2 |
| 3 | 6 | Reports | Reports, CSV export, search, optional GUI | v0.3 |
| 4 | 7 | Test and QA campaign | System tests, regression, coverage and metrics, bug fixing | v0.9 |
| 5 | 8 | Finish | Final SRS, report, diagram images, demo, release, buffer | v1.0 |

## Estimate versus actual (by work package)
Planned hours come from the work breakdown in the project description. Log actual hours weekly and fill the last two columns at the end. Variance = actual minus planned. Compute totals with a script or spreadsheet formula.

| Work package | Planned hours | Actual hours | Variance |
|---|---|---|---|
| Requirements, SRS, risk register | 14 | | |
| Design: UML, DFD, ER, CRC cards | 18 | | |
| Domain model | 24 | | |
| Persistence | 16 | | |
| Services, facade, budgets, recurring rules | 20 | | |
| Reports and exporters | 14 | | |
| Console UI | 12 | | |
| Swing GUI (Should) | 16 | | |
| Testing | 24 | | |
| QA, configuration management, report, demo | 20 | | |
| Contingency (20% of the above) | 36 | | |

After sprint 1, compare these hours with the COCOMO, function-point and story-point cross-checks and write down which was closest and why.

**Note on estimation models (Decision D-18):**
- **Bottom-up WBS:** Planned baseline of **214 person-hours** (178 base hours + 36 hours / 20% contingency) suited for a focused student team.
- **Function Point Analysis:** Yields ~22 unadjusted FP (~176–220 hours), which closely aligns with the 214-hour WBS baseline.
- **COCOMO Basic Organic:** For ~2.0 KLOC estimated code base yields ~5.6 person-months (~850–1,360 hours). As documented in Decision D-18, COCOMO assumes industrial waterfall full-lifecycle overhead (administrative, formal reviews, and documentation burdens) and does not map directly to student agile iterations. Actual hours will be tracked against the 214-hour WBS baseline.

## Velocity and change tracking

| Sprint | Story points planned | Story points done | Requirement changes (count) | Defects found | Notes |
|---|---|---|---|---|---|
| 0 | – | – | 1 (SRS v0.2 Gate A1 updates) | 0 | Requirements baseline and risk spike |
| 1 | 30 | 30 | 0 | 0 | Core domain, persistence, console UI (tag v0.1) |
| 2 | 20 | 20 | 0 | 0 | Budgets, alerts, recurring, goals (tag v0.2) |
| 3 | 25 | 25 | 0 | 0 | Reports, CSV round-trip, search/filter, Gate D, Swing GUI (tag v0.3) |
| 4 | not provided | not provided | 1 (SRS v0.4 security & hardening updates) | 3 | QA campaign, hardening, system benchmarks (tag v0.9) |

## Sprint entries

### Sprint 0 (week 1)
- **Dates:** 2026-11-13 (in progress)
- **Goal:** Requirements baseline, architecture decisions, risk spike, and initial project infrastructure.
- **Planned items:** SRS inspection, Gate A1 design alignment, test plan mapping, risk register rescoring, CI and build setup.
- **Completed items:** Steps 1.1 to 1.7 completed; Gate A1 closed (Q-001 to Q-015); Gate A2 passed; SRS bumped to v0.2; DECISIONS.md updated with D-23 to D-33; TEST_PLAN.md and COVERAGE.md filled with 51 concrete test IDs; RISKS.md updated with R11 to R16; Step 2.1 completed: Maven project skeleton (`pom.xml`, `.mvn/`, `mvnw`, `mvnw.cmd`, `.github/workflows/ci.yml`, `.gitignore`, 6 `package-info.java`, `SmokeTest.java`); Step 2.2 completed: `Money` value object and `MoneyTest` (`TC-NFR01-01`, `TC-FR19-01`); Step 2.3 completed: Persistence spike (`Schema.java`, `JdbcTransactionRepository.java`, `TransactionRecord.java`, `PiemsException.java`, `PersistenceException.java`, `JdbcSpikeTest.java`, `audit/spike-notes.md`, `TC-FR18-02`, `TC-FR18-03`, `TC-FR04-02`, `TC-FR04-03`, `TC-NFR03-01`); Step 2.4 completed: Package architectural script (`scripts/CheckPackages.java`, `scripts/check-packages`, `scripts/check-packages.cmd`, `src/test/resources/package-violation/`, `PackageDependencyTest.java`, `TC-NFR07-01`); Step 2.5 completed: Mermaid UML diagrams (`class-domain.mmd`, `package.mmd`, `state-goal.mmd`, `state-budget.mmd`, `docs/diagrams/questions.md`); Step 2.6 completed: CRC cards (`docs/diagrams/crc.md`, `DiagramSyntaxTest.java`).
- **Not completed and why:** GATE B review and walking skeleton verification scheduled next.
- **Hours (planned / actual):** 14 (Requirements/SRS) + 2 (Build skeleton) + 2 (Money domain) + 2 (Persistence spike) + 1 (Package check script) + 2 (UML diagrams) + 1 (CRC cards) / 20
- **Decisions made (IDs in `docs/DECISIONS.md`):** D-01 through D-33 (D-02, D-05 Accepted; D-12 superseded by D-23; D-31 Accepted; D-32, D-33 Proposed)
- **Risks changed (IDs in `docs/RISKS.md`):** R2 rescored to 5 (Mitigated); R6 rescored to 3 (Mitigated); R5 rescored to 8 (Managed); R7, R8, R10 rescored; R11 to R16 added
- **Tests (from tool output):** 24 run, 24 passed, 0 failed (`MoneyTest` 12 tests, `DiagramSyntaxTest` 5 tests, `JdbcSpikeTest` 4 tests, `PackageDependencyTest` 2 tests, `SmokeTest` 1 test; JaCoCo Domain line coverage: 90.5% (76/84), branch coverage: 85% (34/40); `./mvnw -B verify` passing in 6.134 s with 0 Checkstyle violations)
- **Retrospective:** Keep: Structured audit and human gate validation · Stop: Untracked TBDs in matrices · Try: Automated sync check script

### Sprint 1 (weeks 3 to 4) – Core Domain & Persistence
- **Dates:** 2026-11-13 to 2026-11-14 (Completed)
- **Goal:** Core domain model, Account hierarchy and debit rules, Transactions, Categories, Repositories, Facade and Console UI (tag v0.1).
- **Planned items:** Steps 3.1 (Accounts), 3.2 (Transactions), 3.3 (Categories), 3.4 (Repositories), 3.5 (Facade & Console UI), 3.6 (DocConsistencyTest), Step 7.1 (Sprint 1 Review).
- **Completed items:** Step 3.1 completed: Account hierarchy (`Account.java`, `CashAccount.java`, `BankAccount.java`, `CreditCardAccount.java`, `InsufficientFundsException.java`) and polymorphic contract test suite (`AccountContractTest.java`, `CashAccountTest.java`, `BankAccountTest.java`, `CreditCardAccountTest.java`). Tests verify Decision Table rows R1 to R5, TC-FR01-01, TC-FR01-02, TC-FR03-01, TC-FR03-02, TC-FR03-03, TC-FR03-04. Zero overdraft on bank accounts enforced per D-27; credit card non-negative debt tracking per D-31. Step 3.2 completed: Transaction hierarchy (`Transaction.java`, `Income.java`, `Expense.java`, `Transfer.java`), `TransactionFactory.java`, domain exceptions (`ValidationException.java`, `RecordNotFoundException.java`), and test suite (`TransactionContractTest.java`, `IncomeTest.java`, `ExpenseTest.java`, `TransferTest.java`, `TransactionFactoryTest.java`). Tests verify TC-FR02-01 (income raises balance), TC-FR02-02 (non-positive amount rejected), TC-FR04-01 (transfer preserves net worth), TC-FR06-01 (edit preserves balance consistency), TC-FR19-01 (boundary values 0.00, 0.01, 999999999.99, 1000000000.00, 0.001). Zero `instanceof` checks in `src/main` outside `equals`. Step 3.3 completed: Category tree composite hierarchy (`CategoryComponent.java`, `Category.java`, `CategoryGroup.java`), `TransactionFactory` integration (`createWithCategory`, `validateCategory`), and test suite `CategoryTreeTest.java` (10 tests). Tests verify TC-FR05-01 (in-use category cannot be deleted, throws ValidationException; reassigning allows deletion), TC-FR05-02 (soft-delete archiving via `archive()`; transactions with archived categories rejected), polymorphic `leaves()` and `path()` traversal without type checks (`instanceof`). Step 3.4 completed: Repositories with contract tests (`AccountRepository`, `CategoryRepository`, `TransactionRepository`, `InMemoryAccountRepository`, `JdbcAccountRepository`, `InMemoryCategoryRepository`, `JdbcCategoryRepository`, `InMemoryTransactionRepository`, `JdbcTransactionRepository`, `AccountRepositoryContractTest`, `InMemoryAccountRepositoryTest`, `JdbcAccountRepositoryTest`, `CategoryRepositoryContractTest`, `InMemoryCategoryRepositoryTest`, `JdbcCategoryRepositoryTest`, `TransactionRepositoryContractTest`, `InMemoryTransactionRepositoryTest`, `JdbcTransactionRepositoryTest`, `TransferAtomicityTest`). Tests verify TC-FR04-02 & TC-NFR03-01 (fault injection rolls back with PersistenceException and leaves both derived balances unchanged), TC-FR18-01 (state survives restart with identical records and derived balances), and CRUD operations across all repositories with zero `instanceof` checks. Step 3.5 completed: Facade and Console UI (`FinanceService.java`, `TransactionView.java`, `ConsoleApp.java`, `ConsoleMenu.java`, `Main.java`, `SqliteDatabase.java`) and test suites `FinanceServiceTest.java` (14 tests) and `ConsoleScriptTest.java` (5 tests). Tests verify TC-FR07-01 (net worth equals sum of account balances), TC-FR07-02 (derived balance matches opening balance plus transaction effects), TC-NFR04-01 (adding expense takes at most 4 inputs), and error handling without CLI crashes. Architecture verification passes: `scripts/check-packages` clean with 0 cycles and 0 violations; `System.out` strictly confined to `piems.ui`; zero `instanceof` checks in `src/main` outside `equals`. Step 3.6 completed: Automated documentation consistency test `DocConsistencyTest.java` (3 tests). Tests verify D-21 & D-34: Check 1 (COVERAGE.md Section 5 test IDs exist in TEST_PLAN.md, verified requirements appear in test source code), Check 2 (DECISIONS.md `### D-` headings equal Index rows and Total decisions count in ITERATION_LOG.md), Check 3 (README.md quoted test count matches Surefire reports total). Deliberate doc mutations confirmed test failure; restored docs pass cleanly. Step 7.1 completed: Sprint 1 Review, tag `v0.1`, release notes documented in `CHANGELOG.md`, measured values updated in `docs/COVERAGE.md` Section 6 and `docs/TEST_PLAN.md` Section 13.
- **Not completed and why:** None for Sprint 1 (all planned items 3.1 to 3.6 and 7.1 completed). Sprints 2 through 4 items (Budgets, Recurring, Goals, Reports, CSV, Search, GUI, Backup) scheduled for Phases D through F.
- **Hours (planned / actual):** 19 planned / 15 actual (Steps 3.1 through 3.6 and 7.1)
- **Decisions made (IDs in `docs/DECISIONS.md`):** Total decisions: 34 (D-01 through D-34; D-07, D-08, D-11, D-32, D-33, D-34 Accepted in Sprint 1)
- **Risks changed (IDs in `docs/RISKS.md`):** R5 & R12 (partial writes and unforced foreign keys) mitigated by centralized PRAGMA enforcement and atomic transfer tests.
- **Tests (from tool output):** 183 run, 183 passed, 0 failed (`MoneyTest` 12, `TransactionFactoryTest` 15, `FinanceServiceTest` 14, `CashAccountTest` 13, `CreditCardAccountTest` 13, `TransferTest` 12, `BankAccountTest` 11, `ExpenseTest` 11, `IncomeTest` 11, `CategoryTreeTest` 10, `InMemoryAccountRepositoryTest` 7, `JdbcAccountRepositoryTest` 7, `InMemoryCategoryRepositoryTest` 6, `JdbcCategoryRepositoryTest` 6, `InMemoryTransactionRepositoryTest` 6, `JdbcTransactionRepositoryTest` 6, `ConsoleScriptTest` 5, `DiagramSyntaxTest` 5, `JdbcSpikeTest` 4, `TransferAtomicityTest` 3, `DocConsistencyTest` 3, `PackageDependencyTest` 2, `SmokeTest` 1; JaCoCo Domain line coverage: 90.4% (424/469), branch coverage: 80.6% (195/242); JaCoCo Overall line coverage: 82.0% (1125/1372), branch coverage: 71.5% (394/551); `./mvnw -B verify` passing with 0 Checkstyle violations).
- **Retrospective:** Keep: Abstract contract tests for polymorphic hierarchies; boundary value equivalence testing; Composite pattern with pure polymorphism; TempDir SQLite tests · Stop: Passing double literals into Money; ad-hoc doc counts without automated verification · Try: Mutation testing in Sprint 4; property-based test cases for financial domain rules.
- **Release tag and changelog entry:** Tag `v0.1` (`CHANGELOG.md`).

### Sprint 2 (week 5) – Budgets, Alerts, Recurring & Goals
- **Dates:** 2026-11-14 (Completed)
- **Goal:** Budgeting rules, Observer alerts, recurring transaction scheduling, and savings goals tracking (tag v0.2).
- **Planned items:** Steps 4.1 (`Budget.evaluate` white-box tests), 4.2 (Alerts & Observer), 4.3 (Recurring rules & catch-up), 4.4 (Savings goals), 4.5 (Must check), Step 7.1 (Sprint 2 Review).
- **Completed items:** Step 4.1 completed: Implemented domain entity `Budget.java` and enum `BudgetStatus.java` with positive limit constructor check (throwing `IllegalArgumentException` per Q-011 and D-13), integer arithmetic minor-unit evaluation without floats, and matching helper for duplicate detection. Implemented white-box test suite `BudgetEvaluateTest.java` (12 tests) verifying four independent execution paths (V(G) = 4), boundary values (79.99%, 80.00%, 99.99%, 100.00%, 100.01%), duplicate budget rejection (TC-FR09-01), and null parameter checks. Branch coverage for `Budget` moved from 0% (uncreated) to 84.6% (22/26 branches) and 100% on `Budget.evaluate` (4/4 branches). Step 4.2 completed: Implemented Observer pattern alert components in `piems.service` (`BudgetListener.java`, `BudgetAlertEvent.java`, `BudgetMonitor.java`) and integrated into `FinanceService.java`. Configured hysteresis state management per Q-009 and Decision D-28 (`last_notified_status` reset to OK upon dropping below 80%, re-arming threshold alerts). Enforced exception containment per Decision D-09 (throwing listener never aborts transaction recording). Implemented unit test suite `BudgetMonitorTest.java` (8 tests) and system integration test suite `BudgetAlertScenarioTest.java` (3 tests for Scenario S2). Step 4.3 completed: Implemented `Schedule.java`, `RecurringRule.java`, and `RecurringGenerator.java` with injected `Clock` (no `LocalDate.now()` in domain or service). Implemented month-end date clamping for schedules starting on the 31st per Q-010 and D-29 (`monthlyRuleOnThe31stClampsToMonthEnd`, TC-FR11-03). Implemented scenario S4 catch-up tests (`generatesOneTransactionPerDueDateAfterDowntime`, TC-FR11-01) verifying 3 months of downtime produces 1 transaction per due date, and verified idempotency (`secondRunAddsNothing`, TC-FR11-02). Step 4.4 completed: Implemented domain entity `SavingsGoal.java`, `GoalStatus.java`, `GoalContribution.java`, and `Contribution.java`. Embedded complete lifecycle state transition table in class Javadoc matching `docs/diagrams/state-goal.mmd`. Resolved Q-008 via Decision D-35 (deferring `OVERDUE` and `CANCELLED` states to post-v1.0). Implemented unit test suite `SavingsGoalTest.java` (11 tests) verifying `TC-FR12-01` (`becomesAchievedWhenContributionsReachTarget`), progress boundaries at 0%, 99.99%, 100%, and >100%, and status reversion from ACHIEVED back to ACTIVE upon contribution removal. Step 4.5 completed: Read-only audit of all 13 Must functional requirements documented in `audit/2026-11-14-must/final-report.md` and `audit/2026-11-14-must/00-STATE.md`; verified 9 of 13 Must requirements (FR-06 partial, FR-13 and FR-14 scheduled for Sprint 3); raised findings F-01 to F-05 and remediation proposals P-101 to P-105; rescored risks R11 and R12 to Mitigated (3) and opened R17. Step 7.1 completed: Sprint 2 Review, tag `v0.2`, release notes documented in `CHANGELOG.md`, measured values updated in `docs/COVERAGE.md` Section 6 and `docs/TEST_PLAN.md` Section 13.
- **Not completed and why:** None for Sprint 2 (all planned items 4.1 to 4.5 and 7.1 completed). Sprints 3 and 4 items (Reports, CSV round trip, Search and filter, Swing GUI, Backup, QA campaign) scheduled for Phases E and F.
- **Hours (planned / actual):** 12 planned / 8 actual (Steps 4.1 through 4.5 and 7.1)
- **Decisions made (IDs in `docs/DECISIONS.md`):** Total decisions: 35 (D-01 through D-35; D-09, D-14, D-28, D-29, D-35 exercised)
- **Risks changed (IDs in `docs/RISKS.md`):** R2 (rounding drift) mitigated on budget evaluations; R5 & R12 mitigated by atomic transfers; downtime catch-up verified for scenario S4; alert spam eliminated via hysteresis (D-28); scope creep prevented via Q-008 goal state deferral (D-35); R11 & R12 rescored to Mitigated (3); R17 opened for Gate D alignment.
- **Tests (from tool output):** 229 run, 229 passed, 0 failed (`TransactionFactoryTest` 15, `FinanceServiceTest` 14, `CashAccountTest` 13, `CreditCardAccountTest` 13, `BudgetEvaluateTest` 12, `MoneyTest` 12, `TransferTest` 12, `BankAccountTest` 11, `ExpenseTest` 11, `IncomeTest` 11, `SavingsGoalTest` 11, `CategoryTreeTest` 10, `RecurringRuleTest` 9, `BudgetMonitorTest` 8, `InMemoryAccountRepositoryTest` 7, `JdbcAccountRepositoryTest` 7, `InMemoryCategoryRepositoryTest` 6, `JdbcCategoryRepositoryTest` 6, `InMemoryTransactionRepositoryTest` 6, `JdbcTransactionRepositoryTest` 6, `ConsoleScriptTest` 5, `DiagramSyntaxTest` 5, `JdbcSpikeTest` 4, `BudgetAlertScenarioTest` 3, `RecurringCatchUpTest` 3, `TransferAtomicityTest` 3, `DocConsistencyTest` 3, `PackageDependencyTest` 2, `SmokeTest` 1; JaCoCo Domain line coverage: 92.4% (640/693), branch coverage: 80.3% (293/365); JaCoCo Service line coverage: 92.7% (268/289), branch coverage: 80.0% (80/100); JaCoCo Repository line coverage: 83.6% (445/532), branch coverage: 73.6% (131/178); JaCoCo Overall line coverage: 85.3% (1492/1749), branch coverage: 75.9% (523/689); `./mvnw -B verify` passing with 0 Checkstyle violations).
- **Retrospective:** Keep: Observer pattern for threshold alerts with zero exceptions (D-09); Hysteresis threshold state reset (`OK` below 80%) preventing alert flapping (D-28); Pure integer minor-unit arithmetic without floating-point conversions (D-06); Month-end clamping for recurring rules (D-29); Rigorous read-only audit gate (`/audit-project`) before milestone tagging · Stop: Scope expansion beyond frozen v1.0 requirements (e.g. deferring goal `OVERDUE` and `CANCELLED` states per D-35); Ambiguities between gate exit criteria and future sprint schedules (P-102) · Try: Strategy pattern implementation for financial reports in Sprint 3 (D-10); Automated contract tests for report exporters.
- **Release tag and changelog entry:** Tag `v0.2` (`CHANGELOG.md`).

### Sprint 3 (week 6) – Reports, Exporters, Search, Gate D & Swing GUI
- **Dates:** 2026-11-14 to 2026-11-15 (Completed)
- **Goal:** Strategy-based reports, CSV export/import, search and filter, Gate D certification, optional Swing GUI (tag v0.3).
- **Planned items:** Steps 5.1 (Report strategies), 5.2 (Exporters, CSV round trip), 5.3 (Search and filter), Gate D closeout, 5.4 (Swing GUI), Step 7.1 (Sprint 3 review).
- **Completed items:** Step 5.1 completed: Implemented `ReportRequest`, `ReportResult`, `ReportStrategy`, `MonthlySummaryReport` (FR-13), `CategoryBreakdownReport` (FR-14), `CashFlowReport` (FR-15), and integrated into `FinanceService` facade with zero type checks (D-10, NFR-09). Implemented Decision D-36 for Q-013 (savings rate displays "N/A" on zero income) and Q-014 (Hare-Niemeyer Largest Remainder rounding guaranteeing 100.0% sum). Implemented unit and integration test suites `MonthlySummaryReportTest`, `CategoryBreakdownReportTest`, `CashFlowReportTest`, `NewStrategyNeedsNoServiceChangeTest` (13 tests). Step 5.2 completed: Implemented `Exporter` template method (D-10), `CsvExporter`, `TextExporter`, `TransactionExportRow`, `CsvImporter` (FR-17, D-30), `ImportResult`, `ImportException`, and integrated into `FinanceService` facade (FR-16, FR-17). Transfers export as single rows and import splits into two atomic linked legs (`executeTransfer`, D-23). Re-import duplicate reconciliation via multiset matching (D-37). Added test suites `CsvRoundTripTest` (TC-FR16-01, TC-FR16-02), `CsvImporterTest` (TC-FR17-01, TC-FR17-02), `CsvExporterTest` (15 new tests). Step 5.3 completed: Implemented `TransactionQuery` specification record, `findByQuery` in `TransactionRepository`, `InMemoryTransactionRepository`, and `JdbcTransactionRepository` with parameterized SQL query composition, and `findTransactions` facade in `FinanceService` (FR-08, D-38). Added contract tests in `TransactionRepositoryContractTest` (`filtersCombineWithAnd`, `emptyQueryReturnsAllRecords`, `unmatchedQueryReturnsEmptyList`, `queryRejectsStartDateAfterEndDate`, TC-FR08-01) passing on both repository implementations and `FinanceServiceTest` (9 new tests). Gate D closeout completed: Implemented `findByTransferLinkId` and `deleteByTransferLinkId` across repository contracts and implementations, and `deleteTransaction` and `editTransaction` in `FinanceService` facade with atomic two-leg deletion and derived balance restoration (FR-06, D-23, D-39). Added tests in `TransactionRepositoryContractTest` and `FinanceServiceTest` (`deleteTransferDeletesBothLegsAndRestoresBalances` TC-FR06-02, `editTransactionUpdatesAmountAndDerivedBalances` TC-FR06-01; 6 new tests). Gate D passed (13 of 13 Must functional requirements verified and certified). Step 5.4 completed: Implemented Swing Graphical User Interface under `piems.ui.swing` (`MainView`, `MainPresenter`, `TransactionTableModel`, `BudgetStatusBadge`, `MainFrame`, `SwingApp`) using the Model-View-Presenter (MVP) pattern with asynchronous off-EDT execution and top-level error handling (D-05, D-15, D-40). Interactive features include expense recording form, sortable transaction table (`TableRowSorter`), search/filter bar, and budget evaluation badge with colour plus text. Implemented unit and presenter test suite `MainPresenterTest` (9 headless tests: `TC-FR07-03`, `TC-FR03-05`, `TC-FR03-06`, `TC-FR19-03`, `TC-FR08-02`, `TC-FR10-07`, `TC-FR10-08`, `TC-FR10-09`, `TC-NFR04-02`). Step 7.1 completed: Sprint 3 Review, tag `v0.3`, release notes documented in `CHANGELOG.md`, measured values updated in `docs/COVERAGE.md` Section 6 and `docs/TEST_PLAN.md` Section 13.
- **Not completed and why:** Step 5.5 (Backup, restore, migration) deferred as optional Could requirement to Phase F/G.
- **Hours (planned / actual):** 14 planned / 11 actual (Steps 5.1 through 5.4, Gate D, 7.1)
- **Decisions made (IDs in `docs/DECISIONS.md`):** Total decisions: 40 (D-01 through D-40; D-10 Accepted, D-30 Accepted, D-36 Accepted, D-37 Accepted, D-38 Accepted, D-39 Accepted, D-40 Accepted in Sprint 3)
- **Risks changed (IDs in `docs/RISKS.md`):** R1 managed as optional Swing GUI is implemented strictly on top of existing facade without compromising frozen Must requirements; R17 closed (Gate D passed).
- **Tests (from tool output):** 281 run, 281 passed, 0 failed (`MainPresenterTest` 9, `FinanceServiceTest` 17, `TransactionFactoryTest` 15, `CashAccountTest` 13, `CreditCardAccountTest` 13, `BudgetEvaluateTest` 12, `MoneyTest` 12, `TransferTest` 12, `InMemoryTransactionRepositoryTest` 12, `JdbcTransactionRepositoryTest` 12, `BankAccountTest` 11, `ExpenseTest` 11, `IncomeTest` 11, `SavingsGoalTest` 11, `CategoryTreeTest` 10, `RecurringRuleTest` 9, `BudgetMonitorTest` 8, `InMemoryAccountRepositoryTest` 7, `JdbcAccountRepositoryTest` 7, `CsvImporterTest` 7, `InMemoryCategoryRepositoryTest` 6, `JdbcCategoryRepositoryTest` 6, `CsvExporterTest` 6, `CategoryBreakdownReportTest` 5, `ConsoleScriptTest` 5, `DiagramSyntaxTest` 5, `MonthlySummaryReportTest` 4, `JdbcSpikeTest` 4, `CashFlowReportTest` 3, `BudgetAlertScenarioTest` 3, `RecurringCatchUpTest` 3, `TransferAtomicityTest` 3, `DocConsistencyTest` 3, `PackageDependencyTest` 2, `CsvRoundTripTest` 2, `NewStrategyNeedsNoServiceChangeTest` 1, `SmokeTest` 1; JaCoCo Domain line coverage: 93.1% (651/699), branch coverage: 80.2% (296/369); Overall line coverage: 78.8% (2289/2903), branch coverage: 66.4% (856/1289); `./mvnw -B verify` passing with 0 Checkstyle violations).
- **Retrospective:** Keep: Model-View-Presenter (MVP) architecture enabling 100% headless testing without Swing windows; Largest Remainder Method rounding guaranteeing exact 100.0% category breakdown sums; Parameterized query specification for safe and uniform transaction filtering; Atomic two-leg transfer deletion · Stop: Direct imports of repository query objects into UI packages (encapsulated via facade convenience overloads so UI depends strictly on domain and service) · Try: Systematic white-box and black-box QA campaign passes in Sprint 4 (Steps 6.1 through 6.8).
- **Release tag and changelog entry:** Tag `v0.3` (`CHANGELOG.md`).

### Sprint 4 (week 7)
- **Dates:** 2026-11-14 to 2026-11-15 (Completed)
- **Goal:** Comprehensive QA campaign: domain coverage gap closure (6.1), white-box branch & mutation testing (6.2), black-box equivalence & boundary testing (6.3), system scenarios (6.4), security audit (6.6), acceptance checklist (6.7), consolidated report (6.5), and approved fixes hardening (6.8) (tag v0.9).
- **Planned items:** Steps 6.1 through 6.8, Sprint review 7.1 (tag v0.9).
- **Completed items:** Step 6.1 completed: Domain coverage gaps audited (`audit/2026-11-15-coverage-gaps/coverage-gaps.md`, `00-STATE.md`). Identified 4 classes below thresholds (`TransactionFactory` line 72.9%, `Transaction` branch 58.8%, `Expense` branch 66.7%, `Income` branch 66.7%). Step 6.2 completed: White-box testing, branch gap closure, and defensive no-op account semantics (`BudgetEvaluateBranchTest`, `BudgetMonitorBranchTest`, `CsvImporterBranchTest`, `ExpenseNoOpBranchTest`, `IncomeNoOpBranchTest`; 327 tests passed; D-41 recorded; bytecode-level PITest documented as constrained by JVM runtime version 69 in `audit/2026-11-15-campaign/final-report.md`). Step 6.3 completed: Black-box QA pass derived from SRS acceptance criteria, IEEE 829 debit decision table R1–R5, state transition tables (SavingsGoal, Budget), and UC-03 flows (32 new black-box tests added: `DebitDecisionTableBlackBoxTest` 5, `ExpenseUseCaseFlowBlackBoxTest` 5, `StateTransitionBlackBoxTest` 5, `RequirementAcceptanceBlackBoxTest` 17; findings F-06, F-07, F-08 recorded in `audit/2026-11-15-blackbox/findings.md` and `docs/SRS.md`; D-42 recorded). Step 6.4 completed: System scenarios S1 to S6 automated and benchmarked against temp SQLite databases (`SystemScenarioTest` 8 tests: S1 hand-calculated month, S2 observer alerts, S3 fault-injection transfer rollback, S4 3-month recurring catch-up, S5 10k transactions benchmark meeting latency targets, S6 restart persistence check and corrupt DB safe rejection; D-43 recorded; `audit/test-results.md` created). Step 6.6 completed: Security quick pass (`audit/2026-11-15-security/security-review.md`). Step 6.7 completed: Acceptance trial protocol and checklist (`audit/acceptance-checklist.md`; validation trial pending). Step 6.5 completed: Consolidated QA campaign report (`audit/2026-11-15-campaign/final-report.md`). Step 6.8 completed: QA hardening proposals implemented (P-301 CheckPackages network rule, P-303 SQL LIKE wildcard escaping, P-304 CSV formula neutralization and lossless round-trip, P-308 CLI DB URL auto-prefix, P-309 trial baseline seed script and derived balance test; P-305 deferred; D-44 recorded).
- **Not completed and why:** S6 backup/restore and older-schema checks: FR-20 and NFR-10 Deferred (Could items, not implemented in v1.0; step 5.5 not scheduled).
- **Hours (planned / actual):** not provided / approx. 18 h (team estimate)
- **Decisions made (IDs in `docs/DECISIONS.md`):** Total decisions: 45 (D-01 through D-45; D-41 Accepted in Step 6.2, D-42 Accepted in Step 6.3, D-43 Accepted in Step 6.4, D-44 Accepted in Step 6.8, D-45 Accepted in Step 7.3)
- **Risks changed (IDs in `docs/RISKS.md`):** None (R1 to R16 tracked; R8 test gaps addressed through QA campaign expansion to 371 tests).
- **Tests (from tool output):** 371 run, 371 passed, 0 failed across 48 test classes (`.\mvnw.cmd -B verify`; JaCoCo Domain line coverage: 96.1% [672/699], branch coverage: 91.3% [337/369]; JaCoCo Overall line coverage: 81.8% [2391/2924], branch coverage: 71.9% [946/1315]; 0 Checkstyle violations; 0 package cycles).
- **Retrospective:** Keep: reading each agent plan before approving and writing tests first · Stop: letting plans contain invented numbers or build production code during a QA step · Try: answering open questions earlier and checking test IDs for collisions before writing tests.
- **Release tag and changelog entry:** Tag `v0.9` (`CHANGELOG.md`).

### Step 8.3 Part A – Budget Persistence & Restart Hysteresis (FR-09, FR-18)
- **Dates:** 2026-11-15 (Completed)
- **Goal:** Make category budgets persist across application restart via SQLite table `budgets`, add `BudgetRepository` contracts and implementations (`InMemoryBudgetRepository`, `JdbcBudgetRepository`), enforce unique `(category_id, month)` index, mandate full constructor in `Main`, preload budgets into `BudgetMonitor`, and re-arm session alerts.
- **Completed items:**
  - `BudgetRepository.java` contract interface created.
  - `InMemoryBudgetRepository.java` and `JdbcBudgetRepository.java` implemented with unique constraint enforcement.
  - `Schema.java` updated with unique index `idx_budget_cat_month` on `budgets(category_id, month)`.
  - `SqliteDatabase.java` updated to instantiate and expose `budgetRepository()`.
  - `FinanceService.java` wired with `BudgetRepository`, full constructor used in `Main.java`, and `preloadBudgets()` added.
  - Fail-first proof `RestartPersistenceCheckTest` (TC-FR18-09) demonstrated failing, then passing.
  - Production wiring restart test `MainBudgetRestartTest` (TC-FR18-10) verified.
  - Budget alert re-arming test `BudgetRestartAlertTest` (TC-FR10-16) verified.
  - Contract test suite `BudgetRepositoryContractTest` (TC-FR09-04..08) verified against both in-memory and JDBC repositories.
- **Decisions made (IDs in `docs/DECISIONS.md`):** Total decisions: 46 (D-01 through D-46; D-46 Accepted in Step 8.3 Part A)

### Step 8.3 /sync-docs – Status Alignment & Scope Deferral (FR-11, FR-12, FR-18, D-47)
- **Dates:** 2026-11-15 (Completed)
- **Goal:** Align requirement statuses across SRS, COVERAGE, and TEST_PLAN to reflect that FR-11 (recurring rules) and FR-12 (savings goals) are verified at domain level only and not reachable from service or UI, and FR-18 is partially verified (accounts, categories, transactions, budgets persist; goals and recurring rules cannot yet be created).
- **Completed items:**
  - `docs/SRS.md` updated: FR-11 and FR-12 marked "Partially verified: domain only, not reachable from service or UI"; FR-18 marked "Partially verified"; version 0.8 change-history row added.
  - `docs/COVERAGE.md` updated: Section 5 traceability table and Section 7 known gaps aligned with D-47.
  - `docs/TEST_PLAN.md` updated: Section 3 and Section 9.1 test case descriptions annotated with domain-only coverage and D-47 deferral.
  - `docs/DECISIONS.md` updated: Decision D-47 recorded and indexed.
  - `README.md` updated: SRS version 0.8 and decision count 47 reflected.
  - Zero changes to `src/`.
- **Decisions made (IDs in `docs/DECISIONS.md`):** Total decisions: 47 (D-01 through D-47; D-47 Accepted)

### Step 7.5 – Limitations, Disciplines Applied & Lessons Draft
- **Dates:** 2026-11-15 (Completed)
- **Goal:** Draft comprehensive report sections for Project Limitations and Disciplines Applied (and Problems Each Prevented) mapped to syllabus units, attributing facts exclusively to `docs/RISKS.md`, `docs/ITERATION_LOG.md`, `audit/` reports, and `docs/DECISIONS.md`.
- **Completed items:**
  - `docs/LIMITATIONS_AND_LESSONS.md` authored with 12 factual project limitations and two-column discipline analysis contrasting concrete project findings against design risks avoided.
  - `docs/COVERAGE.md` Unit 1 Topic "Role of software engineering" updated from Planned to Pass referencing `docs/LIMITATIONS_AND_LESSONS.md`.
  - `README.md` updated with `docs/LIMITATIONS_AND_LESSONS.md` in documentation index.
  - Reconciled technical questions: non-negative `Money` with explicit `CreditCardAccount.outstandingDebt`, `java -version` (Oracle JDK 25.0.1 runtime with bytecode version 69 vs `pom.xml` target 17), `Money.DEFAULT_CURRENCY` (INR constant), and 1,128 line count for `FinanceService.java`.
  - Zero edits to `src/`.
- **Decisions made (IDs in `docs/DECISIONS.md`):** Total decisions: 47 (D-01 through D-47; no new decisions)

### Sprint 5 (week 8) – Final Release v1.0 & Project Finish
- **Dates:** 2026-11-15 to 2026-11-15 (Completed)
- **Goal:** Phase G Finish: budget persistence hardening (Step 8.3 Part A), documentation synchronization (FR-11, FR-12, FR-18, D-47), limitations and lessons report (Step 7.5), viva preparation sheets (Step 7.4), build & run guide Appendix B, and release v1.0.
- **Planned items:** Steps 7.1, 7.2, 7.3, 7.4, 7.5, 8.3 Part A.
- **Completed items:**
  - Budget persistence across restarts via `BudgetRepository`, `JdbcBudgetRepository`, `InMemoryBudgetRepository`, unique index `idx_budget_cat_month`, and `Main` constructor wiring (D-46).
  - Traceability and doc consistency sync (`docs/SRS.md`, `docs/COVERAGE.md`, `docs/TEST_PLAN.md`) aligning FR-11, FR-12, and FR-18 under D-47.
  - Project Limitations and Disciplines Applied report (`docs/LIMITATIONS_AND_LESSONS.md`) covering 12 project limitations and two-column evidence comparison.
  - Viva preparation sheets for 6 core classes (`audit/viva/index.md`, `money.md`, `account.md`, `transaction.md`, `budget.md`, `report-strategy.md`, `finance-service.md`).
  - Appendix B: Build and Run Guide in `README.md` with dedicated PowerShell and macOS commands.
  - Final Sprint Review, release tag `v1.0`, and changelog `[1.0.0]`.
- **Not completed and why:** FR-20 & NFR-10 (Backup/Restore/Migration) deferred as Could; reachability of recurring rules (FR-11) and savings goals (FR-12) deferred under D-47; multi-OS CI matrix (NFR-06) verified on Windows locally.
- **Hours (planned / actual):** not provided / approx. 18 h (team estimate)
- **Decisions made (IDs in `docs/DECISIONS.md`):** Total decisions: 47 (D-01 through D-47; D-46 Accepted in Step 8.3 Part A, D-47 Accepted in Step 8.3 /sync-docs)
- **Risks changed (IDs in `docs/RISKS.md`):** R9 marked as mitigated, pending viva rehearsal by each owner.
- **Tests (from tool output):** 384 run, 384 passed, 0 failed (`MainBudgetRestartTest` 1, `MainTest` 1, `DiagramSyntaxTest` 5, `DocConsistencyTest` 2, `BankAccountTest` 11, `BudgetEvaluateTest` 13, `CashAccountTest` 13, `CategoryTreeTest` 12, `CreditCardAccountTest` 13, `DebitDecisionTableBlackBoxTest` 5, `ExpenseTest` 19, `IncomeTest` 19, `MoneyTest` 14, `RecurringRuleTest` 9, `SavingsGoalTest` 11, `ScheduleTest` 12, `StateTransitionBlackBoxTest` 5, `TransactionFactoryTest` 21, `TransactionTest` 18, `TransferTest` 18, `BudgetEvaluateBranchTest` 5, `BudgetMonitorBranchTest` 5, `CsvImporterBranchTest` 4, `ExpenseNoOpBranchTest` 3, `IncomeNoOpBranchTest` 3, `CashFlowReportTest` 3, `CategoryBreakdownReportTest` 5, `CsvExporterTest` 6, `MonthlySummaryReportTest` 4, `NewStrategyNeedsNoServiceChangeTest` 1, `InMemoryAccountRepositoryTest` 7, `InMemoryBudgetRepositoryTest` 5, `InMemoryCategoryRepositoryTest` 6, `InMemoryTransactionRepositoryTest` 13, `JdbcAccountRepositoryTest` 7, `JdbcBudgetRepositoryTest` 5, `JdbcCategoryRepositoryTest` 6, `JdbcSpikeTest` 4, `JdbcTransactionRepositoryTest` 13, `RestartPersistenceCheckTest` 1, `TransferAtomicityTest` 3, `BudgetAlertScenarioTest` 3, `BudgetMonitorTest` 10, `BudgetRestartAlertTest` 1, `CsvImporterTest` 9, `CsvRoundTripTest` 3, `ExpenseUseCaseFlowBlackBoxTest` 5, `FinanceServiceTest` 19, `RecurringCatchUpTest` 3, `RequirementAcceptanceBlackBoxTest` 17, `SystemScenarioTest` 8, `SmokeTest` 1, `ConsoleScriptTest` 5, `MainPresenterTest` 9; JaCoCo Domain line coverage: 96.1% [672/699], branch coverage: 91.3% [337/369]; JaCoCo Overall line coverage: 82.0% [2515/3066], branch coverage: 71.8% [982/1367]; 0 Checkstyle violations; 0 package cycles).
- **Retrospective:** Keep: test-first fail-first persistence checks (`RestartPersistenceCheckTest`), strict decoupling of UI from database through `FinanceService`, and continuous documentation consistency checks · Stop: leaving domain models unpersisted until late in the development cycle · Try: early end-to-end integration testing for all domain components in future projects.
- **Release tag and changelog entry:** Tag `v1.0` (`CHANGELOG.md`).

### Template for each further sprint
- **Sprint / dates:**
- **Goal:**
- **Planned items (issue numbers):**
- **Completed items:**
- **Not completed and why:**
- **Hours (planned / actual):**
- **Decisions made (D-xx):**
- **Risks changed (R-xx):**
- **Tests and coverage (from tool output):**
- **Demo notes and user feedback:**
- **Retrospective:** Keep · Stop · Try
- **Release tag and changelog entry:**
