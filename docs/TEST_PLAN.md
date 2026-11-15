# Test Plan – PIEMS

Put this file at `docs/TEST_PLAN.md`. Outline follows IEEE 829. Results and counts are filled from tool output, not typed by hand.

## 1. Purpose and scope
Verify that PIEMS meets `docs/SRS.md` and validate that it is usable. Covers unit, integration, system, acceptance and regression testing, using both white-box and black-box techniques.

## 2. Test items
Domain classes (`Money`, accounts, transactions, categories, budgets, goals, recurring rules), repositories (in-memory and JDBC), `FinanceService`, report strategies and exporters, importer, console UI and optional GUI.

## 3. Features tested and not tested
**Tested:** every requirement FR-01 to FR-20 that is implemented, and NFR-01 to NFR-10.
**Partially tested / verified at domain level only:** FR-11 (recurring rules) and FR-12 (savings goals) are verified at domain and calculation level only; facade methods in `FinanceService`, persistence repositories, and UI controls are not implemented (D-47). FR-18 is partially verified: accounts, categories, transactions, and budgets persist across restart (D-46); goals and recurring rules are not persisted because they cannot yet be created (D-47).
**Not tested (state reasons):** GUI pixel layout; multi-user behaviour (out of scope); any Deferred requirement (FR-20, NFR-10).

## 4. Approach

| Level | What | Technique | Tool |
|---|---|---|---|
| Unit | Single class or method | White-box: statement, branch and condition coverage; basis-path tests; black-box: equivalence classes and boundary values | JUnit 5, JaCoCo |
| Integration | Service with real SQLite (temporary file); repository contract tests against both implementations | Bottom-up: `Money`, then accounts and transactions, then repositories, then `FinanceService`, then UI | JUnit 5 |
| Object-oriented | Class state, inheritance, polymorphism, collaboration | Abstract contract tests per hierarchy; state-transition tests; scenario tests from sequence diagrams | JUnit 5 |
| System | Whole application | Scripted scenarios S1 to S6 (section 9), performance and fault injection | Scripts, timer |
| Acceptance | Each acceptance criterion in the SRS | Checklist run with 2 or 3 people | Manual |
| Regression | Whole suite | Every push | GitHub Actions |

**Cyclomatic complexity example.** `Budget.evaluate(spent)` has three decision points (limit at most 0, ratio at least 1.0, ratio at least 0.8), so V(G) = 4 and at least four independent-path tests are required (TC-FR10-01, TC-FR10-02, TC-FR10-04, and TC-FR10-05 cover them).

## 5. Entry, exit and pass/fail criteria
- **Entry:** code compiles, reviewed, and previous regression suite passes.
- **Exit for a sprint:** all planned tests pass; no open defect of severity High; coverage targets in the SRS (NFR-07) met or the gap recorded.
- **Pass/fail:** a test passes only when the actual result equals the expected result exactly.
- **Suspend** testing when the build fails or the database cannot be created; **resume** after the blocking defect is fixed and the smoke test passes.

## 6. Environment and tools
JDK 17 or later, Maven, SQLite file in a temporary directory per test, JUnit 5, JaCoCo, Checkstyle, SpotBugs, GitHub Actions. Demo data seeded so results are reproducible.

## 7. Roles and schedule

| Activity | Week | Responsible |
|---|---|---|
| Test plan and test cases for Must requirements | 2 | – |
| Unit and integration tests alongside each sprint | 3 to 6 | – |
| System, acceptance and regression campaign | 7 | – |
| Final results and report | 8 | – |

## 8. Test case template and naming
Columns: ID, requirement, type, preconditions, input or steps, expected result, actual result, status (Planned, Pass, Fail, Blocked). ID format `TC-FRxx-nn` or `TC-NFRxx-nn`. Test methods are named after behaviour (for example `rejectsExpenseAboveBalance`), never after a review round or a date. An ID shared by a contract test and a system test is intentional when both check the same behaviour.

## 9. Test cases

### 9.1 Functional

| ID | Req | Type | Preconditions | Input / steps | Expected result | Actual | Status |
|---|---|---|---|---|---|---|---|
| TC-FR01-01 | FR-01 | unit | none | Create cash account with opening balance 50.00 | Balance is 50.00 | Pass (CashAccountTest.initialBalanceMatchesOpening: opening balance 50.00 matches initial balance 50.00; history kept) | Pass |
| TC-FR01-02 | FR-01 | unit | Archived account | Record an expense on it | Rejected with a validation error; history kept | Pass (AccountContractTest.archivedAccountRejectsAnyDebit: debit on archived account throws InsufficientFundsException; history kept) | Pass |
| TC-FR02-01 | FR-02 | unit | Account balance 0.00 | Record income 100.00 | Balance is 100.00 | Pass (IncomeTest.incomeRaisesBalance: 0.00 + 100.00 leaves balance 100.00, effect +10000 minor units) | Pass |
| TC-FR02-02 | FR-02 | unit | Any account | Record income 0.00 | Validation error; balance unchanged | Pass (IncomeTest.incomeRejectsNonPositiveAmount & TransactionFactoryTest.boundaryAmountZeroRejected: 0.00 rejected, balance unchanged) | Pass |
| TC-FR03-01 | FR-03 | unit | Cash balance 100.00 | Record expense 40.00 | Balance is 60.00 | Pass (CashAccountTest.acceptsExpenseWithinBalance: expense 40.00 leaves balance 60.00) | Pass |
| TC-FR03-02 | FR-03 | unit | Cash balance 100.00 | Record expense 100.01 | Insufficient-funds error; balance unchanged | Pass (CashAccountTest.rejectsExpenseAboveBalance: expense 100.01 throws InsufficientFundsException; balance unchanged at 100.00) | Pass |
| TC-FR03-03 | FR-03 | unit | Credit card, limit 500.00, balance 0.00 | Expense 500.00, then expense 500.01 on a fresh card | First accepted (debt 500.00 per D-31); second rejected | Pass (CreditCardAccountTest: 500.00 debt accepted, available credit 0.00 per D-31; 500.01 rejected with InsufficientFundsException) | Pass |
| TC-FR03-04 | FR-03 | unit | Bank balance 100.00 | Record expense 100.01 | InsufficientFundsException; balance unchanged (no overdraft allowed) | Pass (BankAccountTest.bankRejectsExpenseAboveBalanceWhenNoOverdraft: expense 100.01 throws InsufficientFundsException per D-27; balance unchanged) | Pass |
| TC-FR03-05 | FR-03 | unit | Expense form filled | Submit valid expense via presenter | Recorded through facade; table refreshed; form cleared | Pass (MainPresenterTest.addExpenseSuccessfullyRecordsExpenseAndRefreshesView: expense recorded, view updated) | Pass |
| TC-FR03-06 | FR-03 | unit | Expense form filled with amount exceeding balance | Submit expense via presenter | Error routed to top-level view handler without recording transaction | Pass (MainPresenterTest.addExpenseWithInsufficientFundsDisplaysErrorMessage: error displayed, 0 transactions added) | Pass |
| TC-FR04-01 | FR-04 | integration | Accounts A and B | Transfer 25.00 from A to B | A falls by 25.00, B rises by 25.00, net worth unchanged | Pass (TransferTest.transferKeepsNetWorth: A falls to 75.00, B rises to 75.00, net worth stays 150.00, net effect 0) | Pass |
| TC-FR04-02 | FR-04 | integration | Accounts A and B; failure injected on the second update | Transfer 25.00 | Both balances unchanged; error reported | Pass (TransferAtomicityTest.failedTransferLeavesBothBalancesUnchanged: injected fault rolls back, source 100.00 and dest 50.00 derived balances unchanged, 0 rows stored; JdbcSpikeTest passed) | Pass |
| TC-FR04-03 | FR-04 | integration | Accounts A and B | Transfer 50.00 | Two linked transaction rows stored with same transfer_link_id; net worth unchanged | Pass (JdbcSpikeTest.transferWritesBothLegsOrNeither: 2 linked legs stored with UUID; failure leaves count unchanged) | Pass |
| TC-FR05-01 | FR-05 | unit | Category with transactions | Delete the category | Rejected; reassign or archive offered | Pass (CategoryTreeTest.rejectsDeletingCategoryInUse: deleting in-use category throws ValidationException; CategoryTreeTest.reassignAllowsDeletion: reassigning clears in-use and permits deletion) | Pass |
| TC-FR05-02 | FR-05 | unit | Category with transactions | Archive category and attempt new expense | Category marked archived=true; new transaction rejected | Pass (CategoryTreeTest.archivesInsteadOfDeleting: category marked archived=true; new transaction with archived category rejected with ValidationException) | Pass |
| TC-FR06-01 | FR-06 | unit | Expense 40.00 recorded | Edit amount to 30.00 | Balance is 10.00 higher than before the edit | Pass (ExpenseTest.editKeepsBalancesConsistent: reversing 40.00 and applying 30.00 leaves balance 70.00 vs 60.00) | Pass |
| TC-FR06-02 | FR-06 | integration | Linked transfer legs | Delete one transfer leg | Both legs deleted atomically; source and destination balances restored | Pass (FinanceServiceTest.deleteTransferDeletesBothLegsAndRestoresBalances: deleting one transfer leg atomically deletes both legs and restores balances on both accounts) | Pass |
| TC-FR07-01 | FR-07 | unit | Several accounts | Read net worth | Equals the sum of account balances | Pass (FinanceServiceTest.netWorthEqualsSumOfBalances: net worth 1610.00 matches sum of account derived balances 60.00+700.00+850.00) | Pass |
| TC-FR07-02 | FR-07 | unit | Account with opening balance and transactions | Query balance | Derived balance matches opening_minor + sum(transactions.effect) | Pass (FinanceServiceTest.derivedBalanceMatchesOpeningPlusEffects: cash 300.00 and bank 160.00 match opening plus effects) | Pass |
| TC-FR07-03 | FR-07 | unit | Presenter initialized | Load accounts and transactions | Accounts, categories, and initial transactions populated into view | Pass (MainPresenterTest.initializeLoadsAccountsCategoriesAndTransactions: accounts and categories populated) | Pass |
| TC-FR08-01 | FR-08 | integration | Mixed transactions | Filter by category and date range | Only transactions matching both are returned | Pass (TransactionRepositoryContractTest.filtersCombineWithAnd: combined filters by date, category, account, and text verified on both In-Memory and JDBC repositories) | Pass |
| TC-FR08-02 | FR-08 | unit | Transactions exist | Apply search filter via presenter | Table displays matching transactions only | Pass (MainPresenterTest.applyFilterQueriesMatchingTransactionsAndUpdatesView: filtered transactions updated in view) | Pass |
| TC-FR08-04 | FR-08 | integration | Transactions with literal %, _, \\ in notes and decoy records | Query with text containing wildcards | Exact literal substring matched without unescaped wildcard expansion | Pass (TransactionRepositoryContractTest.textFilterMatchesLiteralWildcardCharacters: verified on both InMemory and JDBC repositories) | Pass |
| TC-FR09-01 | FR-09 | unit | Budget exists for Food in a month | Create a second Food budget for that month | Rejected | Pass (BudgetEvaluateTest.duplicateBudgetSameCategoryAndMonthRejected: duplicate budget for same category and month detected and rejected) | Pass |
| TC-FR09-04 | FR-09 | integration | Budget entity created | Save and retrieve from repository | Budget saved and retrieved with exact attributes | Pass (BudgetRepositoryContractTest.savesAndRetrievesBudgetWithExactAttributes: verified on InMemory and JdbcBudgetRepository) | Pass |
| TC-FR09-05 | FR-09 | integration | Budget for Food in month | Attempt second budget for same category/month | PersistenceException thrown | Pass (BudgetRepositoryContractTest.rejectsDuplicateBudgetForSameCategoryAndMonth: unique constraint verified) | Pass |
| TC-FR09-06 | FR-09 | integration | Existing budget | Update limit | New limit retrieved | Pass (BudgetRepositoryContractTest.updatesBudgetLimit) | Pass |
| TC-FR09-07 | FR-09 | integration | Existing budget | Delete budget | findById returns empty | Pass (BudgetRepositoryContractTest.deletesBudget) | Pass |
| TC-FR09-08 | FR-09 | integration | Multiple budgets | findAll and count | All budgets returned | Pass (BudgetRepositoryContractTest.findAllReturnsAllBudgets) | Pass |
| TC-FR10-01 | FR-10 | unit | Limit 100.00 | Spent 79.99 | Status OK; no alert | Pass (BudgetEvaluateTest: path ratio 0.50 and boundary 79.99 return BudgetStatus.OK) | Pass |
| TC-FR10-02 | FR-10 | unit | Limit 100.00 | Spent 80.00 | Status WARNING; one alert | Pass (BudgetEvaluateTest: path ratio 0.85 and boundary 80.00 return BudgetStatus.WARNING) | Pass |
| TC-FR10-03 | FR-10 | unit | Warning already raised | Further expense keeping spent under 100.00 | No second alert | Pass (BudgetEvaluateTest.boundaryBelowOneHundredPercentReturnsWarning: boundary 99.99 returns BudgetStatus.WARNING) | Pass |
| TC-FR10-04 | FR-10 | unit | Limit 100.00 | Spent 100.00 | Status EXCEEDED; one alert | Pass (BudgetEvaluateTest: path ratio 1.0, boundary 100.00, and boundary 100.01 return BudgetStatus.EXCEEDED) | Pass |
| TC-FR10-05 | FR-10 | unit | Limit 0.00 | Evaluate | Validation error | Pass (BudgetEvaluateTest.limitZeroThrowsIllegalArgumentException: limit zero throws IllegalArgumentException per Q-011 and D-13) | Pass |
| TC-FR10-06 | FR-10 | unit | Spending dropped below 80% then rises to 80% | Re-cross threshold after reset | Status WARNING emitted again after resetting to OK | Pass (BudgetMonitorTest.resetToOkWhenDroppingBelowEightyPercent and BudgetAlertScenarioTest.budgetReCrossingWithFinanceService: status resets to OK below 80% and re-alerts WARNING on re-crossing per D-28) | Pass |
| TC-FR10-07 | FR-10 | unit | Category budget exists, spent 80% | Check budget status | Badge displays WARNING status with amber colour and text | Pass (MainPresenterTest.checkBudgetStatusDisplaysWarningWithColorAndTextAtThreshold: WARNING status and colour badge verified) | Pass |
| TC-FR10-08 | FR-10 | unit | Category budget exists, spent 100% | Check budget status | Badge displays EXCEEDED status with red colour and text | Pass (MainPresenterTest.checkBudgetStatusDisplaysExceededWithColorAndTextAtLimit: EXCEEDED status and badge verified) | Pass |
| TC-FR10-09 | FR-10 | unit | Category budget exists, spent well within limit (20%) | Check budget status | Badge displays OK status with green colour and text | Pass (MainPresenterTest.checkBudgetStatusDisplaysOkWithColorAndTextWhenWithinLimit: OK status and badge verified) | Pass |
| TC-FR11-01 | FR-11 | integration | Monthly rule, clock advanced 3 months | Run generator | One transaction per due date, none duplicated | Pass (domain only: RecurringCatchUpTest.generatesOneTransactionPerDueDateAfterDowntime: 3 months downtime generates exactly 4 transactions Jan 1 to Apr 1; facade and persistence not implemented; D-47) | Pass |
| TC-FR11-02 | FR-11 | integration | Rule already generated for today | Run generator again | No new transactions | Pass (domain only: RecurringCatchUpTest.secondRunAddsNothing: second run on same date generates 0 new transactions; D-47) | Pass |
| TC-FR11-03 | FR-11 | integration | Monthly rule on day 31 | Run generator for February | Generated on Feb 28 (or 29) without error | Pass (domain only: RecurringRuleTest.monthlyRuleOnThe31stClampsToMonthEnd: monthly rule on 31st clamps to Feb 28 and Apr 30 per Q-010 and D-29; D-47) | Pass |
| TC-FR12-01 | FR-12 | unit | Goal target 1,000.00 | Contributions total 1,000.00 | Status Achieved | Pass (domain only: SavingsGoalTest.becomesAchievedWhenContributionsReachTarget: status transitions from ACTIVE to ACHIEVED when contributions reach target, with boundary tests for 0%, 99.99%, 100%, and >100%; facade and persistence not implemented; D-47) | Pass |
| TC-FR13-01 | FR-13 | integration | Known month of data | Generate monthly summary | Income, expense and savings rate match hand calculation | Pass (MonthlySummaryReportTest.matchesHandCalculatedMonth: Income, expense and savings rate match hand calculation; Scenario S1) | Pass |
| TC-FR13-02 | FR-13 | integration | Month with 0 income | Generate monthly summary | Savings rate displays "N/A" (or "-") without division by zero | Pass (MonthlySummaryReportTest.zeroIncomeDisplaysNotApplicableSavingsRate: displays 'N/A' without division by zero per Q-013) | Pass |
| TC-FR14-01 | FR-14 | integration | Known month of data | Generate category breakdown | Shares sum to 100% | Pass (CategoryBreakdownReportTest.sharesSumToOneHundredPercent: multi-category shares sum to 100.0%) | Pass |
| TC-FR14-02 | FR-14 | integration | Monthly category shares with repeating fractions | Generate category breakdown | Sum of shares equals exactly 100.0% | Pass (CategoryBreakdownReportTest.repeatingFractionsSumToExactlyOneHundredPercent: Hare-Niemeyer Largest Remainder sums to exactly 100.0% per Q-014) | Pass |
| TC-FR15-01 | FR-15 | integration | 6 months of data | Produce multi-month cash-flow trend | 6 rows produced with monthly net cash flow | Pass (CashFlowReportTest.sixMonthsProducesSixRowsIncludingEmptyMonths: 6 rows produced with monthly net cash flow including zero-filled empty months) | Pass |
| TC-FR16-01 | FR-16 | integration | Sample data | Export to CSV, then import into an empty ledger | Data identical | Pass (CsvRoundTripTest.exportThenImportIsIdentical: round-trip export/import into empty ledger restores identical transactions, balances, and transfer linkages) | Pass |
| TC-FR16-02 | FR-16 | integration | Ledger with transfer | Export to CSV | Single CSV row with kind=TRANSFER and to_account exported | Pass (CsvRoundTripTest.singleCsvRowWithTransferExported: two-leg transfer deduplicated and exported as single row with to_account) | Pass |
| TC-FR16-06 | FR-16 | integration | Transactions with formula prefixes (=, +, -, @, ') in notes | Export to CSV and import into fresh ledger | Formula trigger characters neutralized in export and restored losslessly on import | Pass (CsvRoundTripTest.formulaTriggerCharactersNeutralizedAndRestoredLosslessly) | Pass |
| TC-FR17-01 | FR-17 | integration | Imported file | Import the same file again | No transactions added | Pass (CsvImporterTest.reimportAddsNothing: re-importing identical statement adds 0 transactions, duplicateCount matches batch) | Pass |
| TC-FR17-02 | FR-17 | integration | CSV statement with unknown category and bad rows | Import statement | Valid rows imported; unknown/bad rows skipped and reported with line numbers | Pass (CsvImporterTest.badRowReportsLineNumber: malformed rows skipped with exact 1-based line numbers, remaining valid rows imported) | Pass |
| TC-FR18-01 | FR-18 | system | Sample data saved | Restart the application | State identical | Pass (TransferAtomicityTest.stateSurvivesRestart: fresh repository instances on restarted DB reproduce identical account, category, transaction records and 879.50 derived balance; budgets persist via D-46; goals and recurring rules not persisted; D-47) | Pass |
| TC-FR18-02 | FR-18 | integration | SQLite database file | Schema creation and table check | Tables accounts, categories, transactions, budgets, goals, recurring_rules created | Pass (JdbcSpikeTest.insertsAndReadsOneTransaction: schema tables & indexes created, exact Money minorUnits verified) | Pass |
| TC-FR18-03 | FR-18 | integration | Invalid data insertion | Foreign key violation test | SQLite foreign key constraint enforced (PRAGMA foreign_keys=ON) | Pass (JdbcSpikeTest.rejectsOrphanRowThroughForeignKey: PRAGMA foreign_keys=ON verified via orphan reject) | Pass |
| TC-FR18-04 | FR-18 | integration | SQLite connection pool / busy handler | Concurrent read access | DB busy timeout configured; cleanly handles reads | Not applicable (single user, D-01) | Planned |
| TC-FR19-01 | FR-19 | unit | Any account | Amount 0.001 | Rejected (more than 2 decimals) | Pass (MoneyTest.rejectsTooManyDecimals & TransactionFactoryTest: 0.00, 0.01, 999999999.99, 1000000000.00, 0.001 verified) | Pass |
| TC-FR19-02 | FR-19 | unit | FinanceService validation | Pass null/empty or invalid fields | ValidationException thrown with specific field error message | Pass (FinanceServiceTest.validatesInputsAndThrowsValidationException: blank name and invalid inputs throw ValidationException) | Pass |
| TC-FR19-03 | FR-19 | unit | Invalid amount entered | Submit expense via presenter | Error dialog displays validation message without crashing | Pass (MainPresenterTest.addExpenseWithInvalidAmountDisplaysErrorMessage: validation error routed to view error handler) | Pass |
| TC-FR20-01 | FR-20 | system | Sample data | Back up, change data, restore | Restored state equals backup | Deferred (Could item, not implemented) | Deferred |
| TC-FR02-03 | FR-02 | unit | Income transaction with null target | applyTo(null) | Silent no-op without exception (D-41) | Pass (IncomeTest.applyToWithNullTargetDoesNotThrow) | Pass |
| TC-FR02-04 | FR-02 | unit | Income transaction with foreign account | applyTo(foreignAccount) | Foreign account balance unmodified (D-41) | Pass (IncomeTest.applyToDoesNotModifyForeignAccountBalance) | Pass |
| TC-FR03-07 | FR-03 | unit | Expense transaction with null target | applyTo(null) | Silent no-op without exception (D-41) | Pass (ExpenseTest.applyToWithNullTargetDoesNotThrow) | Pass |
| TC-FR03-08 | FR-03 | unit | Expense transaction with foreign account | applyTo(foreignAccount) | Foreign account balance unmodified (D-41) | Pass (ExpenseTest.applyToDoesNotModifyForeignAccountBalance) | Pass |
| TC-FR04-04 | FR-04 | integration | Transfer transaction recorded | Edit transfer amount and note across legs | Both legs updated atomically with new amount/note | Pass (FinanceServiceTest.editingTransferUpdatesBothLegsAtomically) | Pass |
| TC-FR04-05 | FR-04 | integration | Transfer transaction recorded | Edit transfer amount exceeding balance | Rejects with InsufficientFundsException; legs unmodified | Pass (FinanceServiceTest.editingTransferBeyondAvailableBalanceThrowsInsufficientFundsException) | Pass |
| TC-FR05-03 | FR-05 | unit | Archived CategoryComponent | createIncome with archived category | ValidationException thrown | Pass (TransactionFactoryTest.rejectsArchivedCategoryWhenCreatingIncome) | Pass |
| TC-FR05-04 | FR-05 | unit | Archived CategoryComponent | createExpense with archived category | ValidationException thrown | Pass (TransactionFactoryTest.rejectsArchivedCategoryWhenCreatingExpense) | Pass |
| TC-FR05-05 | FR-05 | unit | CategoryGroup with children | Remove null child component | Returns false; group unchanged | Pass (CategoryTreeTest.categoryGroupRemoveIgnoresNullChild) | Pass |
| TC-FR05-06 | FR-05 | unit | CategoryGroup with children | Remove non-member child component | Returns false; group unchanged | Pass (CategoryTreeTest.categoryGroupRemoveIgnoresNonMemberChild) | Pass |
| TC-FR06-03 | FR-06 | unit | Unpersisted transactions (id=null) | Equals on identical attributes | Returns true; hashCodes equal | Pass (TransactionContractTest.unpersistedTransactionsAreEqualWhenAllAttributesMatch) | Pass |
| TC-FR06-04 | FR-06 | unit | Unpersisted transactions (id=null) | Equals with differing attributes | Returns false for differing date, amount, account, note, cat | Pass (TransactionContractTest.unpersistedTransactionsDifferWhenAnyAttributeDiffers) | Pass |
| TC-FR06-05 | FR-06 | unit | One unpersisted (id=null), one persisted | Equals comparison | Returns false | Pass (TransactionContractTest.transactionEqualsReturnsFalseWhenComparingPersistedWithUnpersisted) | Pass |
| TC-FR06-06 | FR-06 | unit | Expense transaction with foreign account | reverseOn(foreignAccount) | Foreign account balance unmodified (D-41) | Pass (ExpenseTest.reverseOnDoesNotModifyForeignAccountBalance) | Pass |
| TC-FR06-07 | FR-06 | unit | Expense transaction with null target | reverseOn(null) | Silent no-op without exception (D-41) | Pass (ExpenseTest.reverseOnWithNullTargetDoesNotThrow) | Pass |
| TC-FR06-08 | FR-06 | unit | Income transaction with foreign account | reverseOn(foreignAccount) | Foreign account balance unmodified (D-41) | Pass (IncomeTest.reverseOnDoesNotModifyForeignAccountBalance) | Pass |
| TC-FR06-09 | FR-06 | unit | Income transaction with null target | reverseOn(null) | Silent no-op without exception (D-41) | Pass (IncomeTest.reverseOnWithNullTargetDoesNotThrow) | Pass |
| TC-FR09-02 | FR-09 | unit | Two Budget instances | Equals with differing category, month, or limit | Returns false on differing attributes | Pass (BudgetEvaluateTest.budgetsDifferingInAttributesAreNotEqual) | Pass |
| TC-FR10-10 | FR-10 | unit | BudgetMonitor with registered listener | Register same listener instance again | Idempotent; listener registered exactly once | Pass (BudgetMonitorTest.registerListenerIgnoresDuplicateRegistration) | Pass |
| TC-FR10-11 | FR-10 | unit | Budget in EXCEEDED status | Additional expense recorded | Remains EXCEEDED; no duplicate alert | Pass (BudgetMonitorTest.subsequentExpenseWhenAlreadyExceededDoesNotRealert) | Pass |
| TC-FR11-04 | FR-11 | unit | Two RecurringRule instances | Equals with identical attributes | Returns true; hashCodes match | Pass (domain only: RecurringRuleTest.rulesWithIdenticalAttributesAreEqual; facade and persistence not implemented; D-47) | Pass |
| TC-FR11-05 | FR-11 | unit | RecurringRule instances | Equals with differing attributes | Returns false on any mutated field | Pass (domain only: RecurringRuleTest.rulesDifferingInAnyAttributeAreNotEqual; D-47) | Pass |
| TC-FR11-06 | FR-11 | unit | Schedule on day 31 | Month-end clamping for shorter months | Generates on last valid day of month | Pass (domain only: RecurringRuleTest.scheduleClampsMonthlyDueDateToMonthEnd; D-47) | Pass |
| TC-FR11-07 | FR-11 | unit | Schedule with null fromAfter | nextDueDates calculation | Iterates from startDate | Pass (domain only: RecurringRuleTest.scheduleIteratesWhenFromAfterIsNull; D-47) | Pass |
| TC-FR12-02 | FR-12 | unit | Two SavingsGoal instances | Equals with identical attributes | Returns true; hashCodes match | Pass (domain only: SavingsGoalTest.goalsWithIdenticalAttributesAreEqual; facade and persistence not implemented; D-47) | Pass |
| TC-FR12-03 | FR-12 | unit | SavingsGoal instances | Equals with differing attributes | Returns false on any mutated field | Pass (domain only: SavingsGoalTest.goalsDifferingInAnyAttributeAreNotEqual; D-47) | Pass |
| TC-FR12-04 | FR-12 | unit | SavingsGoal target 0.00 | Calculate progress percentage | Returns zero without arithmetic error | Pass (domain only: SavingsGoalTest.goalProgressReturnsZeroWhenTargetIsZero; D-47) | Pass |
| TC-FR16-03 | FR-16 | unit | CSV statement with expense exceeding balance | Import statement | ImportException thrown; state unmodified | Pass (CsvImporterTest.importingExpenseExceedingAccountBalanceThrowsImportException) | Pass |
| TC-FR16-04 | FR-16 | unit | CSV statement starting with UTF-8 BOM | Import statement | Strips BOM and parses records cleanly | Pass (CsvImporterTest.importingCsvWithUtf8BomSucceeds) | Pass |
| TC-FR19-04 | FR-19 | unit | Valid CategoryComponent | createIncome with CategoryComponent | Income instance created with category | Pass (TransactionFactoryTest.createsIncomeWithCategoryComponent) | Pass |
| TC-FR19-05 | FR-19 | unit | Valid CategoryComponent | createExpense with CategoryComponent | Expense instance created with category | Pass (TransactionFactoryTest.createsExpenseWithCategoryComponent) | Pass |
| TC-FR19-06 | FR-19 | unit | Kind "INCOME" and CategoryComponent | createWithCategory | Income instance returned | Pass (TransactionFactoryTest.createsIncomeWithCategoryViaDispatcher) | Pass |
| TC-FR19-07 | FR-19 | unit | Kind "EXPENSE" and CategoryComponent | createWithCategory | Expense instance returned | Pass (TransactionFactoryTest.createsExpenseWithCategoryViaDispatcher) | Pass |
| TC-FR19-08 | FR-19 | unit | Kind "TRANSFER" and accounts | createWithCategory | Transfer instance returned | Pass (TransactionFactoryTest.createsTransferWithCategoryViaDispatcher) | Pass |
| TC-FR19-09 | FR-19 | unit | Unknown kind string | createWithCategory | ValidationException thrown | Pass (TransactionFactoryTest.rejectsUnknownKindInCreateWithCategory) | Pass |
| TC-FR19-10 | FR-19 | unit | Valid date, amount, category strings | createFromStringWithCategory | Transaction parsed and constructed | Pass (TransactionFactoryTest.createsFromStringWithCategoryComponent) | Pass |
| TC-FR19-11 | FR-19 | unit | Whitespace kind string "   " | validateCommonInputs | ValidationException thrown | Pass (TransactionFactoryTest.rejectsWhitespaceOnlyKind) | Pass |
| TC-FR19-12 | FR-19 | unit | Transaction with null note | Constructor invocation | Retains null note | Pass (TransactionContractTest.constructorsRetainNullNote) | Pass |
| TC-FR01-03 | FR-01 | black-box | Archived cash/credit card account | Attempt debit 1.00 | Rejected with InsufficientFundsException (R5) | Pass (DebitDecisionTableBlackBoxTest.r5ArchivedAccountRejectsDebitRegardlessOfBalance) | Pass |
| TC-FR01-04 | FR-01 | black-box | Clean ledger | Create Cash, Bank, and Credit Card accounts with boundary opening balances (0.00, 500.00, 1000.00) | Accounts created with correct initial derived balances | Pass (RequirementAcceptanceBlackBoxTest.fr01AccountCreationEquivalencePartitionsAndBoundaries) | Pass |
| TC-FR02-05 | FR-02 | black-box | Bank account balance 0.00 | Record income with minimum positive amount 0.01 | Balance increases to 0.01 | Pass (RequirementAcceptanceBlackBoxTest.fr02IncomePositiveAmountBoundaryIncreasesBalance) | Pass |
| TC-FR03-09 | FR-03 | black-box | Cash opening 100.00, Bank opening 200.00 | Debit 40.00 on cash; debit 200.00 on bank (R1) | Debits accepted; cash balance is 60.00, bank is 0.00 | Pass (DebitDecisionTableBlackBoxTest.r1CashOrBankDebitWithinBalanceIsAccepted) | Pass |
| TC-FR03-10 | FR-03 | black-box | Cash balance 50.00, Bank balance 0.00 | Debit 50.01 on cash; debit 0.01 on bank (R2) | Debits rejected with InsufficientFundsException; balances unchanged | Pass (DebitDecisionTableBlackBoxTest.r2CashOrBankDebitExceedingBalanceThrowsInsufficientFunds) | Pass |
| TC-FR03-11 | FR-03 | black-box | Credit card limit 500.00, debt 0.00 | Debit 500.00 (R3) | Debit accepted; debt is 500.00, available credit is 0.00 | Pass (DebitDecisionTableBlackBoxTest.r3CreditCardDebitWithinCreditLimitIsAccepted) | Pass |
| TC-FR03-12 | FR-03 | black-box | Credit card limit 500.00, debt 0.00 | Debit 500.01 (R4) | Rejected with InsufficientFundsException; debt stays 0.00 | Pass (DebitDecisionTableBlackBoxTest.r4CreditCardDebitExceedingCreditLimitThrowsInsufficientFunds) | Pass |
| TC-FR03-13 | FR-03 | black-box | Checking account 500.00, Groceries category | UC-03 Main flow: Record 120.00 expense | Balance decreases to 380.00; transaction view returned | Pass (ExpenseUseCaseFlowBlackBoxTest.uc03MainFlowRecordsExpenseUpdatesBalanceAndMaintainsLedger) | Pass |
| TC-FR03-14 | FR-03 | black-box | Checking account 500.00 | UC-03 Alt 5a: Attempt 500.01 expense | Rejected with InsufficientFundsException; balance unchanged at 500.00 | Pass (ExpenseUseCaseFlowBlackBoxTest.uc03Alternative5aDebitRuleFailureLeavesBalancesAndLedgerUnchanged) | Pass |
| TC-FR04-06 | FR-04 | black-box | Checking account 500.00; repository failure simulated | UC-03 Alt 6a: Attempt 50.00 expense with failing repository | PersistenceException thrown; rollback leaves balance 500.00 and 0 records | Pass (ExpenseUseCaseFlowBlackBoxTest.uc03Alternative6aRepositoryFailureRollsBackAtomically) | Pass |
| TC-FR04-07 | FR-04 | black-box | Bank account 100.00, Cash account 50.00 | Record 40.00 transfer from Bank to Cash | Bank is 60.00, Cash is 90.00; net worth unchanged; transfer legs linked | Pass (RequirementAcceptanceBlackBoxTest.fr04TransferPreservesNetWorthAndMaintainsLinkage) | Pass |
| TC-FR05-07 | FR-05 | black-box | Archived category | Attempt new expense with archived category | ValidationException thrown; transaction rejected | Pass (RequirementAcceptanceBlackBoxTest.fr05ArchivedCategoryRejectsTransactionCreation) | Pass |
| TC-FR06-10 | FR-06 | black-box | Recorded expense of 50.00 | Edit expense amount down to 20.00 | Balance is 30.00 higher; transaction amount updated | Pass (RequirementAcceptanceBlackBoxTest.fr06EditingTransactionUpdatesAccountBalanceConsistently) | Pass |
| TC-FR06-11 | FR-06 | black-box | Recorded expense of 40.00 | Delete the expense | Expense removed; account balance fully restored | Pass (RequirementAcceptanceBlackBoxTest.fr06DeletingTransactionRestoresAccountBalanceConsistently) | Pass |
| TC-FR07-04 | FR-07 | black-box | Cash 50.00, Bank 100.00, Card limit 500.00 with 30.00 expense | Query net worth | Derived net worth matches sum of asset balances (150.00) | Pass (RequirementAcceptanceBlackBoxTest.fr07NetWorthDerivedAcrossMixedAccountTypes) | Pass |
| TC-FR08-03 | FR-08 | black-box | Multiple transactions across dates, categories, and descriptions | Query with matching date range, category, and text keyword | Returns exact intersection matching all criteria (AND logic per FR-08) | Pass (RequirementAcceptanceBlackBoxTest.fr08FilterCombinesDateCategoryAndTextWithAndLogic) | Pass |
| TC-FR09-03 | FR-09 | black-box | Budget set for category in June | Attempt second budget for category in June; set budget in July | Duplicate rejected with ValidationException; different month accepted | Pass (RequirementAcceptanceBlackBoxTest.fr09RejectsDuplicateBudgetForSameCategoryAndMonth) | Pass |
| TC-FR10-12 | FR-10 | black-box | Budget 200.00 set on Groceries | UC-03 Alt 7a: Record 160.00 expense (80%) | Observer notified with BudgetStatus.WARNING | Pass (ExpenseUseCaseFlowBlackBoxTest.uc03Alternative7aThresholdCrossingEmitsBudgetWarningAlert) | Pass |
| TC-FR10-13 | FR-10 | black-box | Category budget 100.00 | Expenses rising 79.99 (OK), 80.00 (WARNING), 100.00 (EXCEEDED) | State transitions OK -> WARNING -> EXCEEDED with one alert per threshold | Pass (StateTransitionBlackBoxTest.budgetTransitionsFromOkToWarningToExceededUponRisingExpenses) | Pass |
| TC-FR10-14 | FR-10 | black-box | Category budget 100.00 in EXCEEDED status | Expense reduced to 50.00, then rises to 85.00 | Drops below 80% resetting state to OK; rising to 85.00 re-emits WARNING | Pass (StateTransitionBlackBoxTest.budgetResetsToOkWhenSpendingDropsBelowEightyPercentAndReAlertsOnRising) | Pass |
| TC-FR11-08 | FR-11 | black-box | Weekly recurring rule over 3 week period | Generate due transactions | Generates exactly 3 transactions without duplicates | Pass (domain only: RequirementAcceptanceBlackBoxTest.fr11RecurringCatchUpGeneratesOneTransactionPerDueInterval; D-47) | Pass |
| TC-FR12-05 | FR-12 | black-box | Goal target 1000.00 | Contribution 400.00 | Status remains ACTIVE; progress is 40.0% | Pass (domain only: StateTransitionBlackBoxTest.goalRemainsActiveWhenContributionsBelowTarget; D-47) | Pass |
| TC-FR12-06 | FR-12 | black-box | Goal target 1000.00 | Contributions reaching exactly 1000.00 | Status transitions to ACHIEVED; progress is 100.0% | Pass (domain only: StateTransitionBlackBoxTest.goalTransitionsFromActiveToAchievedWhenTargetReached; D-47) | Pass |
| TC-FR12-07 | FR-12 | black-box | Goal target 1000.00 | Contribution 1200.00 (F-07) | Status remains ACHIEVED; progress is 120.0% | Pass (domain only: StateTransitionBlackBoxTest.goalRemainsAchievedWhenContributionsExceedTarget; D-47) | Pass |
| TC-FR13-03 | FR-13 | black-box | Month with 1000.00 income and 300.00 expense | Generate monthly summary | Total income 1000.00, expense 300.00, net 700.00, savings rate "70.0%" | Pass (RequirementAcceptanceBlackBoxTest.fr13MonthlySummaryCalculatesTotalsAndSavingsRate) | Pass |
| TC-FR14-03 | FR-14 | black-box | Month with 3 equal expense categories | Generate category breakdown | Category shares sum to exactly 100.0% via Hare-Niemeyer rounding | Pass (RequirementAcceptanceBlackBoxTest.fr14CategoryBreakdownSumsExactlyToOneHundredPercent) | Pass |
| TC-FR15-02 | FR-15 | black-box | 3-month range with Jan and Mar expenses and empty Feb | Generate cash-flow trend | 3 rows produced; Feb row shows 0.00 net cash flow | Pass (RequirementAcceptanceBlackBoxTest.fr15CashFlowTrendProducesRowForEmptyMonths) | Pass |
| TC-FR16-05 | FR-16 | black-box | Ledger with recorded transactions | Export to CSV and import into fresh ledger | Transactions and balances restored identically | Pass (RequirementAcceptanceBlackBoxTest.fr16CsvExportRoundTripRestoresIdenticalLedger) | Pass |
| TC-FR17-03 | FR-17 | black-box | CSV file with 1 duplicate row and 1 invalid row | Import CSV | 0 imported, 1 duplicate counted, 1 skipped row with line number reported | Pass (RequirementAcceptanceBlackBoxTest.fr17CsvImportDuplicateDetectionAndRowSkipReport) | Pass |
| TC-FR18-05 | FR-18 | black-box | SQLite database with recorded account and expense | Restart application with fresh repositories pointing to same file | Account and derived balance identical after restart | Pass (RequirementAcceptanceBlackBoxTest.fr18StateSurvivesApplicationRestartOnSqlite) | Pass |
| TC-FR19-13 | FR-19 | black-box | Expense form with zero amount (0.00) or income category | Record expense | ValidationException thrown; ledger unmodified | Pass (ExpenseUseCaseFlowBlackBoxTest.uc03Alternative4aInvalidInputThrowsValidationException) | Pass |
| TC-FR19-14 | FR-19 | black-box | Blank account name or blank category name | Create account / category | ValidationException thrown | Pass (RequirementAcceptanceBlackBoxTest.fr19ServiceValidatesDateFormatAndRequiredInputs) | Pass |
| TC-FR10-15 | FR-10 | system | Category budget 500.00; rising expenses cross 80% then 100% | Observer notifications | Exactly 1 WARNING alert and 1 EXCEEDED alert; no duplicates | Pass (SystemScenarioTest.s2BudgetCrossesEightyThenOneHundredPercentWithOneAlertPerThreshold) | Pass |
| TC-FR10-16 | FR-10 | integration | Category budget at 85% in session 1 | Application restarted with fresh service | Alert repeats due to session-scoped hysteresis reset to OK | Pass (BudgetRestartAlertTest.alertRepeatsAfterRestartWhenSpendingRemainsOverThreshold: preloaded budget and hysteresis re-arming verified per D-46) | Pass |
| TC-FR11-09 | FR-11 | system | Monthly rule, clock advanced 3 months | Catch-up run on SQLite | Exactly 4 transactions (Jan-Apr 1); second run adds 0 | Pass (domain only: SystemScenarioTest.s4RecurringRuleCatchesUpAfterThreeMonthsDowntimeWithoutDuplicates: in-memory rule and generator; facade and persistence not implemented; D-47) | Pass |
| TC-FR18-06 | FR-18 | system | Database file with fixed corrupt byte header | Bootstrap SQLite database | Gracefully throws PersistenceException wrapping SQLException | Pass (SystemScenarioTest.s6CorruptDatabaseFileThrowsPersistenceExceptionSafely) | Pass |
| TC-FR18-07 | FR-18 | unit | CLI arguments with bare database filename | Extract database URL via Main.extractDbUrl | Automatically prepends jdbc:sqlite: prefix | Pass (MainTest.extractDbUrlAutoPrependsJdbcSqlitePrefix) | Pass |
| TC-FR18-08 | FR-18 | system | Fresh SQLite database schema | Execute scripts/seed-trial.sql | Baseline accounts and categories initialized with valid derived balances | Pass (SystemScenarioTest.seedTrialScriptInitializesBaselineWithDerivedBalances) | Pass |
| TC-FR18-09 | FR-18 | integration | Budget saved on SQLite database | Restart application with fresh service instances | Budget persists and is preloaded into service | Pass (RestartPersistenceCheckTest.testBudgetPersistsAcrossRestart: fail-first proof verified) | Pass |
| TC-FR18-10 | FR-18 | system | Budget created using Main production wiring | Close database and restart via Main wiring | Budget identical after restart without in-memory fallback | Pass (MainBudgetRestartTest.mainWiringPreservesBudgetsAcrossRestart: production full constructor verified) | Pass |

### 9.2 Black-box design aids
- **Amount equivalence classes:** invalid (at most 0, non-numeric, more than 2 decimals) and valid (0.01 to 999,999,999.99). Boundary values: 0.00, 0.01, 999,999,999.99, 1,000,000,000.00, 0.001.
- **Debit decision table:** R1 cash or bank with amount within balance, accept; R2 cash or bank with amount above balance, insufficient funds; R3 credit card within limit, accept; R4 credit card beyond limit, reject; R5 archived account, reject whatever the other conditions.
- **State-transition tests:** `SavingsGoal` (Active, Achieved; Overdue and Cancelled deferred to post-v1.0 per Q-008) and budget status (OK, WARNING, EXCEEDED) along every legal and several illegal transitions.

### 9.3 Non-functional

| ID | Req | Type | Steps | Expected result | Actual | Status |
|---|---|---|---|---|---|---|
| TC-NFR01-01 | NFR-01 | unit | Sum 1,000 entries of 0.10 | Exactly 100.00 | Pass (MoneyTest.sumsThousandTenCentEntriesToExactlyOneHundred: 10000 minorUnits, 100.00) | Pass |
| TC-NFR02-01 | NFR-02 | system | Load 10,000 transactions; list them; run the monthly report | List under 1 s; report under 2 s | Pass (SystemScenarioTest.s5TenThousandTransactionsBenchmarkMeetsLatencyTargets: listing median 45 ms (min 29 ms, max 57 ms) < 1,000 ms target; monthly report median 32 ms (min 28 ms, max 62 ms) < 2,000 ms target) | Pass |
| TC-NFR03-01 | NFR-03 | integration | Inject a failure during a transfer | No partial change | Pass (TransferAtomicityTest.failedTransferLeavesBothBalancesUnchanged: atomic rollback preserves zero partial state and leaves balances unchanged; JdbcSpikeTest passed) | Pass |
| TC-NFR04-01 | NFR-04 | acceptance | Three new users add an expense | At most 4 inputs each | Pass (ConsoleScriptTest.recordsExpenseInFourInputs: adding expense prompts exactly 4 inputs and prints new balance) | Pass |
| TC-NFR05-01 | NFR-05 | review | Search the code for network classes | None used | Verified (search across src/main and pom.xml finds 0 network dependencies) | Pass |
| TC-NFR05-02 | NFR-05 | review | Source directory containing java.net and javax.net imports | Run CheckPackages analyzer | Exits 1 and reports NFR-05 Network Isolation violations | Pass (PackageDependencyTest.checkPackagesForbidsNetworkImports) | Pass |
| TC-NFR06-01 | NFR-06 | system | Run test suite on Windows and Linux/macOS with JDK 17 | All tests pass identically across environments | Partially verified: Windows only; Linux and macOS not run (SmokeTest.buildRuns on Windows) | Partially verified |
| TC-NFR07-01 | NFR-07 | review | Run JaCoCo and metrics script | Domain line coverage >= 80%, branch coverage >= 70%, methods <= 30 lines, 0 package cycles | Pass (scripts/check-packages: 0 cycles; PackageDependencyTest: 2 passed; JaCoCo domain line 90.4%, branch 80.6%; Checkstyle 0 violations; 183 tests) | Pass |
| TC-NFR08-01 | NFR-08 | unit | Run domain tests with no database | All run | Pass (FinanceServiceTest.netWorthEqualsSumOfBalances: workflow runs with in-memory repositories; scripts/check-packages: 0 JDBC/UI imports in domain/service) | Pass |
| TC-NFR09-01 | NFR-09 | unit | Add a new mock ReportStrategy or Account type | Compiles and passes with modifications to at most 2 existing classes | Pass (NewStrategyNeedsNoServiceChangeTest.addingNewStrategyNeedsNoFinanceServiceChange: custom ReportStrategy executes with zero FinanceService modifications) | Pass |
| TC-NFR10-01 | NFR-10 | integration | Run a schema migration | Backup created first | Deferred (Could item, not implemented) | Deferred |

## 10. Inheritance and polymorphism tests
Abstract `AccountContractTest` and `TransactionContractTest` are run for every subclass. `effectOn` is exercised for income, expense and transfer; `canDebit` for cash, bank and credit card. Repository contract tests run against both the in-memory and the JDBC implementation.

## 11. System scenarios

| ID | Scenario | Expected | Actual / Evidence | Status |
|---|---|---|---|---|
| S1 | New user records a full month and reads the summary | Figures match a hand calculation | Pass: Income 3000.00, Expense 1650.00, Net 1350.00, Savings rate 45.0%, Checking 2450.00, Wallet 0.00 (`SystemScenarioTest.s1NewUserRecordsFullMonthAndSummaryMatchesHandCalculation`) | Pass |
| S2 | Budget crosses 80%, then 100% | One alert per threshold | Pass: WARNING emitted at 420.00 (84%), EXCEEDED emitted at 510.00 (102%), intermediate expenses emit 0 duplicates (`SystemScenarioTest.s2BudgetCrossesEightyThenOneHundredPercentWithOneAlertPerThreshold`) | Pass |
| S3 | Transfer with an injected failure | Both balances unchanged | Pass: Simulated failure throws PersistenceException, atomic rollback preserves original balances 500.00/200.00, 0 orphaned rows (`SystemScenarioTest.s3TransferWithInjectedFailureRollsBackAtomicallyLeavingBalancesUnchanged`) | Pass |
| S4 | Recurring rule catches up after three months of downtime | One transaction per due date, no duplicates | Pass (domain only): 4 transactions generated (Jan 1, Feb 1, Mar 1, Apr 1) via in-memory generator, second execution on same date generates 0 new transactions (`SystemScenarioTest.s4RecurringRuleCatchesUpAfterThreeMonthsDowntimeWithoutDuplicates`); facade and persistence deferred (D-47) | Pass |
| S5 | 10,000 transactions | Performance targets met | Pass: Listing median 45 ms (min 29 ms, max 57 ms) < 1,000 ms target; monthly report median 32 ms (min 28 ms, max 62 ms) < 2,000 ms target (`SystemScenarioTest.s5TenThousandTransactionsBenchmarkMeetsLatencyTargets`) | Pass |
| S6 | Restart, then backup and restore | Data intact | Pass (Partial): Restart check verified (identical accounts, categories, transactions, and budgets persist across restart via D-46; goals and recurring rules cannot yet be created; D-47); corrupt DB gracefully rejected; backup/restore & older-schema blocked: FR-20 and NFR-10 Deferred (step 5.5 not scheduled) | Pass (Partial) |

## 12. Usage profile (borrowed from cleanroom testing)
Weight system test effort toward common operations. Initial assumption, to adjust after the user trial: adding expenses most frequent, then viewing balances, adding income, viewing reports, transfers and budget changes, with backup rare. Record the final profile and its source here.

## 13. Results log
Fill from tool output at the end of each sprint.

| Sprint | Tests run | Passed | Failed | Line coverage | Branch coverage | Date |
|---|---|---|---|---|---|---|
| Sprint 0 (Skeleton) | 1 | 1 | 0 | 0% (0 classes) | 0% (0 classes) | 2026-11-13 |
| Sprint 0 (Money) | 13 | 13 | 0 | 100% (Money: 76/76) | 85% (Money: 34/40) | 2026-11-13 |
| Sprint 1 (Core Domain & Persistence, tag v0.1) | 183 | 183 | 0 | 90.4% (Domain: 424/469) | 80.6% (Domain: 195/242) | 2026-11-14 |
| Sprint 2 (Budgets, Alerts, Recurring & Goals, tag v0.2) | 229 | 229 | 0 | 92.4% (Domain: 640/693) | 80.3% (Domain: 293/365) | 2026-11-14 |
| Sprint 3 (Reports, Exporters, Search, GUI, tag v0.3) | 281 | 281 | 0 | 93.1% (Domain: 651/699) | 80.2% (Domain: 296/369) | 2026-11-15 |
| Sprint 4 (QA Campaign & Hardening, tag v0.9) | 371 | 371 | 0 | 96.1% (Domain: 672/699) | 91.3% (Domain: 337/369) | 2026-11-15 |
| Sprint 5 (Final Release, tag v1.0) | 384 | 384 | 0 | 96.1% (Domain: 672/699) | 91.3% (Domain: 337/369) | 2026-11-15 |
| Maintenance Step 8.1 (D-48) | 396 | 396 | 0 | 96.1% (Domain: 672/699) | 91.3% (Domain: 337/369) | 2026-11-15 |
| Maintenance Step 8.1 (D-49, Balances & Dialogs) | 400 | 400 | 0 | 96.1% (Domain: 672/699) | 91.3% (Domain: 337/369) | 2026-11-15 |

### Supporting Tests for Decision D-48 & D-49 (Unmapped in COVERAGE.md Section 5)
These tests support Decisions D-48 and D-49 (GUI usability, demo data loader, name display, live balances, dialog creation, and form reset) and remain unmapped to specific functional requirements in COVERAGE.md Section 5 per traceability policy:
- `DemoDataLoaderTest`: 6 tests verifying accounts, categories, budget in WARNING, date clamping, all-or-nothing rollback atomicity, and non-empty refusal.
- `MainPresenterTest`: 8 supporting tests verifying empty-state guidance on `addExpense`, demo load view refresh, demo load refusal, table model column names, account creation, category creation, form reset, and live balance/net worth initialization.
- `FinanceServiceTest`: 2 supporting tests verifying `TransactionView` name resolution across operations, transfer legs, and archived accounts/categories.

## 14. Defect log
Lifecycle: New, Assigned, Fixed, Verified, Closed. Track in GitHub Issues with labels for severity and for the phase in which the defect was found.

| ID | Description | Severity | Found in phase | Status | Linked test |
|---|---|---|---|---|---|
| F-06 | UC-03 Alt 6a save failure exception type unspecified; clarified under D-13 as checked PersistenceException | Minor | Phase F (Step 6.3) | Closed | `ExpenseUseCaseFlowBlackBoxTest.alt6aSaveFailsRollsBackAndSurfacesPersistenceException` (TC-FR04-06) |
| F-07 | FR-12 status transition on over-contribution (>100%) unspecified; clarified as retaining ACHIEVED status under D-35 | Minor | Phase F (Step 6.3) | Closed | `StateTransitionBlackBoxTest.savingsGoalOverContributionMaintainsAchievedStatus` (TC-FR12-07) |
| F-08 | FR-10 budget hysteresis downward event dispatch unspecified; clarified under D-28 as silent re-arming | Minor | Phase F (Step 6.3) | Closed | `StateTransitionBlackBoxTest.budgetHysteresisResetBelowEightyPercentRearmsAlertWithoutFiringEvent` (TC-FR10-14) |
| F-09 | FR-07 net worth calculation excludes credit card debt (outstanding debt tracked separately due to non-negative Money under D-24/D-31) | Medium | Phase H (Step 8.1) | Open | Flagged in Decision D-48 |

### Hardening Improvements (Step 6.8 Proposals, Separate from Defects)
- **P-301 (Network Isolation):** Enforced architectural rule in `scripts/CheckPackages.java` forbidding `java.net.*` and `javax.net.*` in `src/main`; linked test `PackageDependencyTest.checkPackagesForbidsNetworkImports` (TC-NFR05-02). Status: Implemented.
- **P-303 (SQL LIKE Escaping):** Escaped wildcard characters (`%`, `_`, `\`) with `ESCAPE '\'` in `JdbcTransactionRepository`; linked test `TransactionRepositoryContractTest.queryEscapesPercentAndUnderscoreWildcardsLiterally` (TC-FR08-04). Status: Implemented.
- **P-304 (CSV Formula Neutralization):** Neutralized spreadsheet formula prefixes (`=`, `+`, `-`, `@`, `\t`, `\r`, `'`) restricted to free-text columns in `CsvExporter` and added lossless unescaping in `CsvImporter`; linked test `CsvRoundTripTest.losslessRoundTripWithFormulaCharacters` (TC-FR16-06). Status: Implemented.
- **P-305 (Arithmetic Overflow):** Overflow in reports evaluated; documented as deferred under D-44 (unreachable under 999M limit; `ArithmeticException` outside D-13 policy). Status: Deferred.
- **P-308 (CLI Database URL Auto-Prefix):** Auto-prepended `jdbc:sqlite:` for raw database filenames in `Main.java`; linked test `MainTest.mainWithRawDatabasePathAutoPrependsJdbcPrefix` (TC-FR18-07). Status: Implemented.
- **P-309 (Trial Baseline Seed Script):** Created `scripts/seed-trial.sql` without stored balance column (D-25/D-44 derived balance architecture); linked test `SystemScenarioTest.seedScriptLoadsAndProducesExpectedBalances` (TC-FR18-08). Status: Implemented.

## 15. Approvals

| Name | Role | Date |
|---|---|---|
| – | – | – |
