# Personal Income & Expense Management System (PIEMS)

PIEMS is a single-user, offline personal income and expense desktop management system developed in Java 17 using Apache Maven and SQLite through JDBC.

---

## 1. Project Overview

PIEMS provides reliable, offline personal accounting:
- **Accounts:** Cash, Bank, and Credit Card accounts with dynamically derived balances.
- **Transactions:** Income, Expense, and atomic two-leg Transfers.
- **Hierarchical Categories:** Recursive category tree with soft-delete archiving.
- **Budgets & Alerts:** Monthly category spending limits with threshold alerts (80% warning, 100% exceeded) via the Observer pattern.
- **Recurring Transactions:** Automated scheduled daily, weekly, and monthly transactions with month-end date clamping.
- **Savings Goals:** Target tracking with progress visualization.
- **Reports:** Monthly summaries, category breakdowns (Hare-Niemeyer percentage rounding), and cash-flow trends.
- **CSV Data Exchange:** Lossless export and statement import with duplicate detection and line-level error reporting.

---

## 2. Architecture & Design Rules

The codebase strictly adheres to the following architectural rules:
1. **Layered Packages:** `piems.domain`, `piems.report`, `piems.service`, `piems.repository`, `piems.ui`, `piems.app`.
2. **Inward Dependencies:** `ui` talks only to `FinanceService` (Facade pattern). No business logic in `ui`. `domain` imports nothing from outer packages.
3. **No Leaky Persistence:** No SQL or JDBC imports outside `piems.repository`. Repositories define clean interfaces with both production JDBC and fast in-memory implementations.
4. **Exact Money Handling:** All financial values are stored and calculated as integer minor units (e.g. paise, cents) inside the immutable `Money` value object. Floating-point types (`double`, `float`) are prohibited for currency.
5. **Deterministic Time:** Injected `java.time.Clock` in domain and service components; direct static calls to `LocalDate.now()` are forbidden.
6. **Alerts via Observer:** Budget threshold alerts use `BudgetListener` observers and never exceptions (D-09, D-28).
7. **Clean Error Handling:** Fail-fast `IllegalArgumentException` for programmer errors; recoverable domain/validation problems use checked `PiemsException` subclasses handled at service and UI boundaries (D-13).
8. **Small Methods & Coverage:** Methods are capped at 30 lines. Minimum coverage targets: 80% domain line coverage, 70% domain branch coverage (NFR-07).
9. **Single-User Offline Security:** Data resides exclusively in a local SQLite file; zero external network access (NFR-05).

---

## 3. Technology Stack

- **Language:** Java 17 LTS
- **Build System:** Apache Maven 3.8+
- **Persistence:** SQLite via SQLite-JDBC
- **Testing:** JUnit 5 (Jupiter), AssertJ
- **Static Analysis & Coverage:** JaCoCo, Checkstyle, SpotBugs
- **CI:** GitHub Actions automated build and test pipeline

---

## 4. Documentation Index

All core documentation lives in the `docs/` directory:
- [docs/SRS.md](file:///c:/Users/muank/Desktop/Sem%203/SE/PROJECT%20SE/docs/SRS.md) – Software Requirements Specification (v0.2, IEEE 29148 structure).
- [docs/DECISIONS.md](file:///c:/Users/muank/Desktop/Sem%203/SE/PROJECT%20SE/docs/DECISIONS.md) – Architectural Decision Records (D-01 through D-44).
- [docs/COVERAGE.md](file:///c:/Users/muank/Desktop/Sem%203/SE/PROJECT%20SE/docs/COVERAGE.md) – Requirements Traceability Matrix and Syllabus Topic Coverage.
- [docs/TEST_PLAN.md](file:///c:/Users/muank/Desktop/Sem%203/SE/PROJECT%20SE/docs/TEST_PLAN.md) – Verification and Validation Test Plan (IEEE 829 structure).
- [docs/RISKS.md](file:///c:/Users/muank/Desktop/Sem%203/SE/PROJECT%20SE/docs/RISKS.md) – Project Risk Register (R1 through R16).
- [docs/ITERATION_LOG.md](file:///c:/Users/muank/Desktop/Sem%203/SE/PROJECT%20SE/docs/ITERATION_LOG.md) – Sprint Logs, Work Breakdown Structure (214 h), and Estimation Cross-Checks.
- `docs/diagrams/` – 4 Mermaid diagrams (`class-domain.mmd`, `package.mmd`, `state-goal.mmd`, `state-budget.mmd`) and 6 CRC cards (`crc.md`) covering domain structure, component responsibilities, package layering, and state lifecycles.

---

## 5. Build and Test Instructions

```bash
# Compile project sources
./mvnw clean compile

# Run all unit and integration tests
./mvnw test

# Full verification: tests, coverage report (JaCoCo), and quality checks
./mvnw verify

# Verify package architecture rules, cycles, and instability (D-05, NFR-07)
./scripts/check-packages        # On Linux/macOS
scripts\check-packages.cmd      # On Windows

# Run console application
./mvnw exec:java -Dexec.mainClass="piems.app.Main"

# Run Swing GUI application (Decision D-40)
./mvnw exec:java -Dexec.mainClass="piems.app.Main" -Dexec.args="--gui"
```

---

## 6. Project Status

- **Phase:** Phase G: Finish (Week 8).
- **Gates:** Gate A1 Closed; Gate A2 Passed; Gate B Passed; Gate D Certified (all 13 Must functional requirements verified and passed); tags `v0.1`, `v0.2`, `v0.3`, and `v0.9` released.
- **Verification:** `./mvnw -B verify` passing (`BUILD SUCCESS`, 372 tests passed, 0 Checkstyle violations).
- **Documentation & Consistency:** Automated consistency verification via `DocConsistencyTest` (Decision D-21, D-34); traceability test IDs verified against test specifications and source annotations; decision count (44) verified across index and log; test counts dynamically verified against Surefire reports.
- **Next Milestone:** Phase G: Finish (Step 7.2 Traceability Completion; FR-20 and NFR-10 Deferred).
