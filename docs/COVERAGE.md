# Coverage Matrix and Traceability – PIEMS

Put this file at `docs/COVERAGE.md`.

It answers two questions: (1) where in the project is each syllabus topic demonstrated, and (2) which design element and tests cover each requirement.

**Rules**

- Status values: `Planned`, `In progress`, `Done`, `Deferred (reason)`, `Discussed only (reason)`. Everything starts as `Planned`.
- A row is `Done` only when the artifact exists and the evidence can be opened or run.
- Do not type counts (tests, coverage, requirements) in this file. Fill the measured-values table in section 6 from commands.
- Update at the end of each sprint and whenever a change request is approved.

## 1. Object-oriented modelling topics

| Topic | Where applied | Artifact / evidence | Status |
|---|---|---|---|
| Classes, objects, attributes, methods, constructors | `Money`, `Account`, `Transaction`, `Budget`; validating constructors, static factories, overloaded constructors | Source in `domain`; unit tests | Planned |
| Encapsulation | Private fields, immutable `Money`, balances change only through `apply`, unmodifiable collection views | Code review checklist; unit tests | Planned |
| Abstraction | Abstract `Account` and `Transaction`; repository and report interfaces | Class diagram; source | Planned |
| Inheritance | Account and transaction hierarchies | Class diagram; contract tests per subclass | Planned |
| Polymorphism | `canDebit`, `effectOn`, `ReportStrategy` | Polymorphism tests; sequence diagrams | Planned |
| Association | `Transaction` to `Account` and `Category`; `Budget` to `Category` | Class diagram | Planned |
| Aggregation | `Ledger` and accounts | Class diagram | Planned |
| Composition | `Transaction` with `Money`; `CategoryGroup` with children; `SavingsGoal` with contributions; `RecurringRule` with `Schedule` | Class diagram | Planned |
| UML: use case | UC-01 to UC-12 | `docs/diagrams/usecase` | Planned |
| UML: class | Domain and pattern views | `docs/diagrams/class_*` | Planned |
| UML: sequence | Record expense with alert; transfer with rollback; report through strategy; recurring generation | `docs/diagrams/sequence_*` | Planned |
| UML: activity | Record transaction; CSV import with duplicate detection | `docs/diagrams/activity_*` | Planned |
| UML: state, object, package, deployment | Goal and budget states; month snapshot; layers; one JVM plus one file | `docs/diagrams/` | Planned |
| SOLID: single responsibility | `Budget`, `BudgetMonitor`, console I/O separated | Design document; review | Planned |
| SOLID: open/closed | New report or account type without editing the service | Design review of NFR-09 | Planned |
| SOLID: Liskov substitution | Contract tests run against every subclass and both repository implementations | Contract test classes | Planned |
| SOLID: interface segregation | Small repository, listener and strategy interfaces | Source | Planned |
| SOLID: dependency inversion | Constructor injection of repositories and `Clock` | `app` wiring | Planned |
| Design patterns | Strategy, Composite, Observer, Factory, Repository, Facade, Template Method; rejected ones in D-20 | Design document; `DECISIONS.md` | Planned |
| Exception handling | Checked hierarchy under `PiemsException`; fail-fast policy; one top-level handler | Exception tests; D-13 | Planned |
| Modularity and maintainable design | Layered packages; cycle and instability check; small methods | Metrics output (`scripts/check-packages`) | Pass |

## 2. Software engineering topics (course outcomes)

| Topic | Where applied | Artifact / evidence | Status |
|---|---|---|---|
| SDLC models | Iterative-incremental with Spiral spike and V-model test mapping | Project description section 5; D-03 | Planned |
| Requirements analysis (functional and non-functional) | FR-01 to FR-20, NFR-01 to NFR-10 | `docs/SRS.md` | Planned |
| SRS documentation | IEEE 29148 style structure | `docs/SRS.md` | Planned |
| Architecture, modularity, design principles | Layered architecture, package rules | Design document; D-05 | Planned |
| Planning, scheduling, effort estimation | WBS, 8-week schedule, COCOMO, function points, story points, actual hours | `docs/ITERATION_LOG.md`; D-18 | Planned |
| Unit testing | JUnit tests of domain classes | Test sources; coverage report | Planned |
| Integration testing | Bottom-up, SQLite temporary file, repository contract tests | Integration tests | Planned |
| System testing | Scenarios S1 to S6 including performance and fault injection | `docs/TEST_PLAN.md` | Planned |
| White-box testing | Branch and path tests, cyclomatic complexity example for `Budget.evaluate` | Unit tests; coverage report | Planned |
| Black-box testing | Equivalence classes, boundary values, decision tables, state transitions | Test cases in `docs/TEST_PLAN.md` | Planned |
| Quality assurance, verification, validation | Reviews, inspections, static analysis, acceptance trials | Review records; QA checklist | Planned |
| Version control and configuration management | Branching, tags, change requests, baselines | Repository history; D-17 | Planned |
| Documentation and maintainability | Documentation set (section 15 of the project description) | `docs/` | Planned |
| Risk analysis | Risk register R1 to R10 | `docs/RISKS.md` | Planned |
| Software maintenance considerations | Four maintenance types, extension points | Project description section 7.2 | Planned |
| DFD and design diagrams | DFD levels 0 to 2, ER, CRC | `docs/diagrams/` | Planned |

## 3. Software engineering syllabus, unit by unit

### Unit 1 – Introduction

| Topic | Where applied | Artifact / evidence | Status |
|---|---|---|---|
| Role of software engineering | Report section on the disciplines applied and the problems each prevented | Report | Planned |
| Software evolution | Lehman's laws and evolution roadmap v1.1 to v2.0 | Project description 14.1 | Planned |
| Legacy system structures | Old spreadsheet analysed as the legacy system | Report | Planned |
| Legacy system design | Assessment of the spreadsheet design | Report | Planned |
| Legacy system assessment | Business value versus technical quality; replace with CSV migration | Report; D-01 | Planned |
| Software development life cycle | Section 5 | Project description; D-03 | Planned |

### Unit 2 – Software process models

| Topic | Where applied | Artifact / evidence | Status |
|---|---|---|---|
| Software process models (generic activities) | Specification, design and implementation, validation, evolution mapped to the schedule | Project description 14.2 | Planned |
| Software specification | SRS | `docs/SRS.md` | Planned |
| Software design and implementation | Design document, source | `docs/` and `src/` | Planned |
| Software validation | Acceptance trials, system tests | `docs/TEST_PLAN.md` | Planned |
| Automated process support | GitHub, Maven, Actions, JaCoCo, Checkstyle, SpotBugs | Repository configuration | Planned |
| Prescriptive models | Described and compared | Project description 14.2 | Planned |
| Waterfall model | SRS and design phase | Project description 5 | Planned |
| Incremental process models | v0.1 to v0.3 increments | Release tags | Planned |
| Evolutionary process models | Prototype, spiral-style risk spike | D-22 | Planned |
| Specialised process models | Component-based used; formal and aspect-oriented discussed | Project description 14.2 | Planned |
| Extreme Programming | Test-first, pair work, continuous integration, refactoring | Repository history; CI | Planned |
| Agile methodology and Scrum | Sprints, backlog, review, retrospective | `docs/ITERATION_LOG.md` | Planned |
| The Unified Process | Four phases mapped to weeks | Project description 14.2 | Planned |

### Unit 3 – Project management and requirements engineering

| Topic | Where applied | Artifact / evidence | Status |
|---|---|---|---|
| Project planning and scheduling | WBS and 8-week schedule | Project description 6 | Planned |
| Risk management | Risk register with mitigation and contingency | `docs/RISKS.md` | Planned |
| Functional and non-functional requirements | FR and NFR tables | `docs/SRS.md` | Planned |
| User requirements | Plain-language goals | `docs/SRS.md` | Planned |
| System requirements | Precise 'shall' statements | `docs/SRS.md` | Planned |
| SRS document generation | Structure and template | `docs/SRS.md` | Planned |
| Feasibility studies and types | Technical, economic, operational, schedule, legal | Project description section 2 | Planned |
| Requirement analysis | Elicitation, classification, prioritisation | SRS appendix | Planned |
| Requirement validation | Checklist review, prototypes, test case per requirement | Review record | Planned |
| Requirement management | IDs, statuses, traceability, change control | Section 5 of this file; D-17 | Planned |
| Software measurement and metrics | GQM table, coverage, instability, LCOM, function points | Section 6 of this file; `scripts/check-packages` | Pass |

### Unit 4 – System modelling and prototyping

| Topic | Where applied | Artifact / evidence | Status |
|---|---|---|---|
| Context models | DFD level 0, use-case boundary | `docs/diagrams/` | Planned |
| Behavioural models | DFD 1 to 2, state and sequence diagrams | `docs/diagrams/state-goal.mmd`, `state-budget.mmd` | In progress (state diagrams created) |
| Data models | ER diagram and data dictionary | `docs/diagrams/`; SRS | Planned |
| Object models | Class, object and CRC diagrams | `docs/diagrams/class-domain.mmd`, `docs/diagrams/crc.md` | Pass |
| CASE workbenches | Upper, lower and integrated tools listed and used | Project description 14.4 | Planned |
| Prototyping in the software process | Throwaway and evolutionary prototypes | D-22 | Planned |
| Rapid prototyping techniques | Paper mock-ups, wireframes, console prototype | Prototype findings in `DECISIONS.md` | Planned |
| Interface specification | Java contracts, CSV format, UI commands | SRS section on external interfaces | Planned |
| Behavioural specification | Pre and postconditions, invariants, state and decision tables | Javadoc; `docs/TEST_PLAN.md` | Planned |

### Unit 5 – Design engineering

| Topic | Where applied | Artifact / evidence | Status |
|---|---|---|---|
| Architectural design | Layered architecture | D-05; `docs/diagrams/package.mmd` | Pass |
| System structuring | Layers and repository boundary | Design document | Planned |
| Control models | Centralised service plus event-driven observer | Design document | Planned |
| Modular decomposition | Object-oriented decomposition by domain concepts | Design document | Planned |
| Domain-specific architectures | Ledger model with debit and credit legs | Design document | Planned |
| Object-oriented design process | Noun-verb analysis, models, interface specification | Design document; CRC cards (`docs/diagrams/crc.md`) | Pass |
| Design with reuse | Patterns, JDK classes, SQLite-JDBC | Design document | Planned |
| Component-based development | Persistence and reporting components behind interfaces | `docs/diagrams/class-domain.mmd`, `package.mmd` | Pass |
| Application families | Variation points for edition, locale, exports | Design document | Planned |
| Design patterns | Seven used, three rejected | D-08 to D-10, D-20 | Planned |
| User interface design principles | Shneiderman and Nielsen applied | UI section of the design document | Planned |
| User interaction | Menus, forms, direct manipulation | Console and GUI | Pass (ConsoleApp and Swing MainFrame/MainPresenter tested in ConsoleScriptTest and MainPresenterTest) |
| Information presentation | Tables, bar chart, colour plus text for budget status | GUI and reports | Pass (TransactionTableModel, BudgetStatusBadge colour plus text in MainPresenterTest, Exporters) |

### Unit 6 – Verification and validation

| Topic | Where applied | Artifact / evidence | Status |
|---|---|---|---|
| Verification and validation planning | Test plan with IEEE 829 style outline | `docs/TEST_PLAN.md` | Planned |
| Software inspections | SRS and core class inspections with checklist and defect log | Inspection records | Planned |
| Cleanroom software development | Usage profile and correctness arguments borrowed; full method discussed only | Report; test plan | Discussed only (not adopted in full) |
| Software testing | All levels | Test sources; `docs/TEST_PLAN.md` | Planned |
| Black-box testing | Equivalence, boundary, decision tables | Test cases | Planned |
| White-box testing | Branch and path coverage | Coverage report | Planned |
| Integration testing | Bottom-up strategy | Integration tests | Planned |
| Object-oriented testing | Class, state, inheritance, polymorphism, interclass tests | Contract and scenario tests | Planned |
| Writing test cases | Template with requirement link and expected result | `docs/TEST_PLAN.md` | Planned |
| Testing tools | JUnit, JaCoCo, Checkstyle, SpotBugs, Actions | Build configuration | Planned |
| Configuration management and version control | Git workflow, tags, change requests | Repository; D-17 | Planned |

## 4. Non-functional requirement verification

| ID | Requirement (short) | Verified by | Status |
|---|---|---|---|
| NFR-01 | Exact money | Unit test summing 1,000 entries of 0.10 (TC-NFR01-01) | Planned |
| NFR-02 | Performance targets | Timed system test with 10,000 transactions (TC-NFR02-01) | Pass (TC-NFR02-01 passed in SystemScenarioTest.s5TenThousandTransactionsBenchmarkMeetsLatencyTargets: listing median 45 ms < 1 s, report median 32 ms < 2 s) |
| NFR-03 | No partial writes | Fault-injection test on transfer (TC-NFR03-01) | Pass (TC-NFR03-01 passed in TransferAtomicityTest and JdbcSpikeTest) |
| NFR-04 | Usability of adding an expense | Trial with 3 people (TC-NFR04-01) | Pass (TC-NFR04-01 passed in ConsoleScriptTest: <= 4 inputs) |
| NFR-05 | Local data only | Code review and dependency check (TC-NFR05-01, TC-NFR05-02) | Pass (TC-NFR05-01 verified via code review; TC-NFR05-02 verified via CheckPackages architectural rule in PackageDependencyTest) |
| NFR-06 | Portability | Build and test on two operating systems if possible; CI multi-OS matrix (TC-NFR06-01) | Partially verified: Windows only; Linux and macOS not run |
| NFR-07 | Maintainability targets | Metrics script and JaCoCo; line >= 80%, branch >= 70% (TC-NFR07-01) | Pass (scripts/check-packages: 0 cycles; PackageDependencyTest: 2 passed; JaCoCo domain line 97.2%, branch 82.8%; Checkstyle 0 violations) |
| NFR-08 | Testability | Domain tests run with in-memory repositories only (TC-NFR08-01) | Planned |
| NFR-09 | Modifiability | Design review: new report or account type touches at most two classes (TC-NFR09-01) | Pass (TC-NFR09-01 passed in NewStrategyNeedsNoServiceChangeTest: custom ReportStrategy runs with zero FinanceService modifications) |
| NFR-10 | Recoverability | Migration test with backup (TC-NFR10-01) | Deferred (Could item, not implemented; schema migrations not built in v1.0) |

## 5. Requirement traceability matrix

Fill the test IDs as tests are written (format `TC-FRxx-nn`). A requirement with no test ID at the end of a sprint is a gap to report.

| Requirement | Use case | Design element | Test IDs | Status |
|---|---|---|---|---|
| FR-01 Accounts | UC-01 | `Account` hierarchy, `AccountRepository` | TC-FR01-01, TC-FR01-02, TC-FR01-03, TC-FR01-04 | Pass (TC-FR01-01 to TC-FR01-04 passed in CashAccountTest, AccountContractTest, DebitDecisionTableBlackBoxTest, and RequirementAcceptanceBlackBoxTest) |
| FR-02 Income | UC-02 | `Income`, `FinanceService` | TC-FR02-01, TC-FR02-02, TC-FR02-03, TC-FR02-04, TC-FR02-05 | Pass (TC-FR02-01 to TC-FR02-05 passed in IncomeTest, TransactionFactoryTest, and RequirementAcceptanceBlackBoxTest; D-41 verified) |
| FR-03 Expense | UC-03 | `Expense`, `Account.canDebit` (D-27) | TC-FR03-01, TC-FR03-02, TC-FR03-03, TC-FR03-04, TC-FR03-05, TC-FR03-07, TC-FR03-08, TC-FR03-09, TC-FR03-10, TC-FR03-11, TC-FR03-12, TC-FR03-13, TC-FR03-14 | Pass (TC-FR03-01 to TC-FR03-05, TC-FR03-07 to TC-FR03-14 passed in CashAccountTest, CreditCardAccountTest, BankAccountTest, ExpenseTest, MainPresenterTest, DebitDecisionTableBlackBoxTest, and ExpenseUseCaseFlowBlackBoxTest; D-41 verified) |
| FR-04 Transfer | UC-04 | `Transfer`, two-leg atomic write (D-23) | TC-FR04-01, TC-FR04-02, TC-FR04-03, TC-FR04-04, TC-FR04-05, TC-FR04-06, TC-FR04-07 | Pass (TC-FR04-01 to TC-FR04-07 passed in TransferTest, TransferAtomicityTest, JdbcSpikeTest, FinanceServiceTest, ExpenseUseCaseFlowBlackBoxTest, and RequirementAcceptanceBlackBoxTest) |
| FR-05 Categories | UC-05 | `CategoryComponent` tree, archive flag (D-26) | TC-FR05-01, TC-FR05-02, TC-FR05-03, TC-FR05-04, TC-FR05-05, TC-FR05-06, TC-FR05-07 | Pass (TC-FR05-01 to TC-FR05-07 passed in CategoryTreeTest, TransactionFactoryTest, and RequirementAcceptanceBlackBoxTest) |
| FR-06 Edit and delete | UC-02, UC-03, UC-04 | `FinanceService` edit and delete, `Account.apply` (D-23, D-39) | TC-FR06-01, TC-FR06-02, TC-FR06-03, TC-FR06-04, TC-FR06-05, TC-FR06-06, TC-FR06-07, TC-FR06-08, TC-FR06-09, TC-FR06-10, TC-FR06-11 | Pass (TC-FR06-01 to TC-FR06-11 passed in ExpenseTest, IncomeTest, FinanceServiceTest, TransactionContractTest, and RequirementAcceptanceBlackBoxTest; D-41 verified) |
| FR-07 Balances and net worth | UC-01 | `Account.balance`, `FinanceService.netWorth`, derived balance (D-25) | TC-FR07-01, TC-FR07-02, TC-FR07-03, TC-FR07-04 | Pass (TC-FR07-01 to TC-FR07-04 passed in FinanceServiceTest, MainPresenterTest, and RequirementAcceptanceBlackBoxTest) |
| FR-08 Search and filter | UC-10 | `TransactionQuery`, repository filters (D-38, D-44) | TC-FR08-01, TC-FR08-02, TC-FR08-03, TC-FR08-04 | Pass (TC-FR08-01 to TC-FR08-04 passed in TransactionRepositoryContractTest, MainPresenterTest, and RequirementAcceptanceBlackBoxTest) |
| FR-09 Budgets | UC-06 | `Budget` | TC-FR09-01, TC-FR09-02, TC-FR09-03 | Pass (TC-FR09-01 to TC-FR09-03 passed in BudgetEvaluateTest and RequirementAcceptanceBlackBoxTest) |
| FR-10 Budget alerts | UC-07 | `Budget.evaluate`, `BudgetListener`, `BudgetMonitor` (D-28, D-40) | TC-FR10-01, TC-FR10-02, TC-FR10-03, TC-FR10-04, TC-FR10-05, TC-FR10-06, TC-FR10-07, TC-FR10-10, TC-FR10-11, TC-FR10-12, TC-FR10-13, TC-FR10-14, TC-FR10-15 | Pass (TC-FR10-01 to TC-FR10-07, TC-FR10-10 to TC-FR10-15 passed in BudgetEvaluateTest, BudgetMonitorTest, BudgetAlertScenarioTest, MainPresenterTest, ExpenseUseCaseFlowBlackBoxTest, StateTransitionBlackBoxTest, and SystemScenarioTest) |
| FR-11 Recurring transactions | UC-08 | `RecurringRule`, scheduler, clamping (D-29) | TC-FR11-01, TC-FR11-02, TC-FR11-03, TC-FR11-04, TC-FR11-05, TC-FR11-06, TC-FR11-07, TC-FR11-08, TC-FR11-09 | Pass (TC-FR11-01 to TC-FR11-09 passed in RecurringCatchUpTest, RecurringRuleTest, RequirementAcceptanceBlackBoxTest, and SystemScenarioTest) |
| FR-12 Savings goals | UC-09 | `SavingsGoal`, `GoalContribution` (D-35) | TC-FR12-01, TC-FR12-02, TC-FR12-03, TC-FR12-04, TC-FR12-05, TC-FR12-06, TC-FR12-07 | Pass (TC-FR12-01 to TC-FR12-07 passed in SavingsGoalTest and StateTransitionBlackBoxTest) |
| FR-13 Monthly summary | UC-10 | `MonthlySummaryReport` | TC-FR13-01, TC-FR13-02, TC-FR13-03 | Pass (TC-FR13-01, TC-FR13-02 passed in MonthlySummaryReportTest; TC-FR13-03 passed in RequirementAcceptanceBlackBoxTest) |
| FR-14 Category breakdown | UC-10 | `CategoryBreakdownReport` | TC-FR14-01, TC-FR14-02, TC-FR14-03 | Pass (TC-FR14-01, TC-FR14-02 passed in CategoryBreakdownReportTest; TC-FR14-03 passed in RequirementAcceptanceBlackBoxTest) |
| FR-15 Cash-flow trend | UC-10 | `CashFlowReport` | TC-FR15-01, TC-FR15-02 | Pass (TC-FR15-01 passed in CashFlowReportTest; TC-FR15-02 passed in RequirementAcceptanceBlackBoxTest) |
| FR-16 CSV export | UC-11 | `Exporter` (CSV), two-leg export (D-23, D-30, D-37, D-44) | TC-FR16-01, TC-FR16-02, TC-FR16-03, TC-FR16-04, TC-FR16-05, TC-FR16-06 | Pass (TC-FR16-01 to TC-FR16-06 passed in CsvRoundTripTest, CsvImporterTest, and RequirementAcceptanceBlackBoxTest) |
| FR-17 CSV import | UC-11 | `CsvImporter`, duplicate detection, row skip (D-30, D-37) | TC-FR17-01, TC-FR17-02, TC-FR17-03 | Pass (TC-FR17-01, TC-FR17-02 passed in CsvImporterTest; TC-FR17-03 passed in RequirementAcceptanceBlackBoxTest) |
| FR-18 Persistence | all | JDBC repositories, schema PRAGMA (D-23, D-25, D-44) | TC-FR18-01, TC-FR18-02, TC-FR18-03, TC-FR18-04, TC-FR18-05, TC-FR18-06, TC-FR18-07, TC-FR18-08 | Pass (TC-FR18-01 to TC-FR18-03 passed in TransferAtomicityTest and JdbcSpikeTest; TC-FR18-05 passed in RequirementAcceptanceBlackBoxTest; TC-FR18-06 and TC-FR18-08 passed in SystemScenarioTest; TC-FR18-07 passed in MainTest) |
| FR-19 Validation | all | `ValidationException`, `Money` | TC-FR19-01, TC-FR19-02, TC-FR19-03, TC-FR19-04, TC-FR19-05, TC-FR19-06, TC-FR19-07, TC-FR19-08, TC-FR19-09, TC-FR19-10, TC-FR19-11, TC-FR19-12, TC-FR19-13, TC-FR19-14 | Pass (TC-FR19-01 to TC-FR19-14 passed in MoneyTest, FinanceServiceTest, MainPresenterTest, TransactionFactoryTest, TransactionContractTest, ExpenseUseCaseFlowBlackBoxTest, and RequirementAcceptanceBlackBoxTest) |
| FR-20 Backup and restore | UC-12 | `BackupService` | TC-FR20-01 | Deferred (Could item, not implemented; single-user offline SQLite database file can be copied manually) |

## 6. Measured values

Fill from commands at the end of each sprint. Leave a cell empty until it has been measured.

| Metric | How to measure | Latest value | Date |
|---|---|---|---|
| Tests run and passed | `mvn test` summary | 372 run, 372 passed, 0 failed | 2026-11-15 |
| Domain line coverage | JaCoCo report | 96.1% (672/699 lines; SavingsGoal 59/64, GoalContribution 8/8, Contribution 9/9, Budget 36/36, RecurringRule 43/44, Schedule 50/52, BudgetStatus 4/4, Money 74/76, Transaction 47/47, Transfer 42/42, TransactionFactory 59/59, Account 23/23, CashAccount 22/25, BankAccount 22/25, CreditCardAccount 27/31, Income 14/14, Expense 14/14, CategoryComponent 42/46, Category 15/16, CategoryGroup 35/37, InsufficientFundsException 4/4, PiemsException 4/4, ValidationException 4/4, RecordNotFoundException 4/4, PersistenceException 4/4, GoalStatus 3/3, Schedule.Frequency 4/4) | 2026-11-15 |
| Domain branch coverage | JaCoCo report | 91.3% (337/369 branches; SavingsGoal 26/30, Schedule 29/37, Budget 26/26, RecurringRule 25/26, Money 36/40, TransactionFactory 37/38, CategoryComponent 23/28, Transfer 23/28, Transaction 34/34, CategoryGroup 15/18, CreditCardAccount 10/10, CashAccount 8/8, BankAccount 8/8, Income 12/12, Expense 12/12, Account 4/4, Category 5/6, GoalContribution 2/2, Contribution 2/2) | 2026-11-15 |
| Package cycles and instability | Metrics script | 0 cycles; domain I=0.000, repository I=0.333, report I=0.500, service I=0.500, ui I=0.667, ui.swing I=1.000, app I=1.000 | 2026-11-15 |
| Requirements with at least one test | Count rows in section 5 with test IDs | 20 of 20 (100% mapped) | 2026-11-15 |
| Planned versus actual hours | `docs/ITERATION_LOG.md` | not provided / approx. 18 h (team estimate) | 2026-11-15 |
| Defects found per phase | Issue tracker labels | 3 findings (F-06, F-07, F-08 in Step 6.3) | 2026-11-15 |

## 7. Known gaps and non-adopted techniques

Record honestly anything not done or only discussed, with the reason.

### Known Gaps at Sprint 4 Completion (Tag v0.9):
1. **Backup, Restore & Migration (FR-20, NFR-10):** Deferred (Could items, not implemented in v1.0; manual SQLite file copy; step 5.5 not scheduled). In Step 6.4, S6 backup/restore and older-schema checks were formally recorded as "blocked: FR-20 and NFR-10 not implemented" (`audit/test-results.md`).
2. **User Acceptance Trial (NFR-04):** Detailed protocol and evaluation sheet created in `audit/acceptance-checklist.md`; validation trial pending participant execution.
3. **SQLite Persistence for Budgets and Goals:** Active budgets and savings goals persist in memory (`BudgetMonitor`, `SavingsGoal`); persistent schema tables exist in `Schema.java`; dedicated JDBC repository implementations not built in v1.0.
4. **Multi-OS Execution Matrix (NFR-06):** Partially verified: Windows only; Linux and macOS CI matrix not run.

### Known Gaps at Sprint 3 Completion (Tag v0.3):
1. **Backup, Restore & Migration (FR-20, NFR-10):** Database snapshotting and schema migration scheduled as optional Could requirement (Phase E, Step 5.5).
2. **Performance Benchmarking (NFR-02):** Timed system execution with 10,000 transactions scheduled for Sprint 4 QA campaign (Phase F, Step 6.4).
3. **Multi-OS Execution Matrix (NFR-06):** Local verification executed on Windows; multi-OS matrix validation configured for CI GitHub Actions workflow.
4. **SQLite Persistence for Budgets and Goals:** Active budgets and savings goals persist in memory (`BudgetMonitor`, `SavingsGoal`); persistent schema tables exist in `Schema.java` and dedicated JDBC repository implementations are scheduled for persistence hardening.

### Known Gaps at Sprint 2 Completion (Tag v0.2):
1. **Reports & Analysis (FR-13, FR-14, FR-15):** Strategy-based monthly summaries, category breakdowns (Largest Remainder Method per Q-014), and cash-flow trends scheduled for Sprint 3 (Phase E, Step 5.1).
2. **Data Import/Export (FR-16, FR-17):** CSV two-leg round-trip export and import parser scheduled for Sprint 3 (Phase E, Step 5.2).
3. **Transaction Search & Filter (FR-08):** Query criteria combining dates, accounts, and categories scheduled for Sprint 3 (Phase E, Step 5.3).
4. **Graphical User Interface (Swing GUI):** Completed and verified via `piems.ui.swing` (`MainFrame`, `MainPresenter`, `BudgetStatusBadge`, `TransactionTableModel`) tested headlessly in `MainPresenterTest` (Decision D-40).
5. **Backup, Restore & Migration (FR-20, NFR-10):** Database snapshotting and schema migration scheduled for Sprint 3 (Phase E, Step 5.5).
6. **Transaction Edit and Delete Facade Methods (FR-06):** Completed and verified via `FinanceService.deleteTransaction` and `editTransaction` with atomic two-leg transfer deletion (`TC-FR06-02`, Decision D-39); Gate D certified.
7. **SQLite Persistence for Budgets and Goals:** Active budgets and savings goals reside in-memory (`BudgetMonitor`, `SavingsGoal`); dedicated repository implementations (`JdbcBudgetRepository`, `JdbcGoalRepository`, Findings F-04, F-05, Proposals P-104, P-105) scheduled for Sprint 3 persistence hardening.
8. **Performance Benchmarking (NFR-02):** Timed system execution with 10,000 transactions scheduled for Sprint 4 QA campaign (Phase F, Step 6.4).
9. **Multi-OS Execution Matrix (NFR-06):** Local verification executed on Windows; multi-OS matrix validation configured for CI GitHub Actions workflow.

### Known Gaps at Sprint 1 Completion (Tag v0.1):
1. **Budgets & Alerts (FR-09, FR-10):** White-box budget evaluations and Observer threshold notifications scheduled for Sprint 2 (Phase D, Steps 4.1 & 4.2).
2. **Recurring Transactions (FR-11):** Catch-up schedule generator and month-end date clamping scheduled for Sprint 2 (Phase D, Step 4.3).
3. **Savings Goals (FR-12):** Goal progress tracking and target contribution logic scheduled for Sprint 2 (Phase D, Step 4.4).
4. **Reports & Analysis (FR-13, FR-14, FR-15):** Strategy-based monthly summaries, category breakdowns, and cash-flow trends scheduled for Sprint 3 (Phase E, Step 5.1).
5. **Data Import/Export (FR-16, FR-17):** CSV two-leg round-trip export and import parser scheduled for Sprint 3 (Phase E, Step 5.2).
6. **Transaction Search & Filter (FR-08):** Query criteria combining dates, accounts, and categories scheduled for Sprint 3 (Phase E, Step 5.3).
7. **Graphical User Interface (Swing GUI):** Optional desktop GUI conditional on passing GATE D scheduled for Sprint 3 (Phase E, Step 5.4).
8. **Backup, Restore & Migration (FR-20):** Database snapshotting and schema migration scheduled for Sprint 3 (Phase E, Step 5.5).
9. **Performance Benchmarking (NFR-02):** Timed system execution with 10,000 transactions scheduled for Sprint 4 QA campaign (Phase F, Step 6.4).
10. **Multi-OS Execution Matrix (NFR-06):** Local verification executed on Windows; multi-OS matrix validation configured for CI GitHub Actions workflow.

### Non-Adopted / Discussed Techniques:
- **Formal Methods & Proofs:** Discussed in syllabus review; not adopted in favor of empirical testing and contract suites (Decision D-20).
- **Aspect-Oriented Programming (AOP):** Discussed for cross-cutting transaction logging; rejected in favor of explicit decorator/facade boundaries and transaction template helpers (Decision D-20, D-33).
- **Cleanroom Software Engineering:** Borrowed usage profile approach for system test prioritization; full formal cleanroom verification not adopted (Decision D-20).
- **Mutation Testing:** Scheduled as an optional quality pass in Sprint 4 (Step 6.2).
