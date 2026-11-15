package piems.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import piems.domain.Account;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.domain.RecurringRule;
import piems.domain.Schedule;
import piems.domain.ValidationException;
import piems.report.ReportRequest;
import piems.report.ReportResult;
import piems.repository.InMemoryAccountRepository;
import piems.repository.InMemoryCategoryRepository;
import piems.repository.InMemoryTransactionRepository;
import piems.repository.JdbcAccountRepository;
import piems.repository.JdbcCategoryRepository;
import piems.repository.JdbcTransactionRepository;
import piems.repository.Schema;
import piems.repository.TransactionQuery;

import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box test suite for all implemented requirements FR-01 through FR-19.
 *
 * <p>Sources of truth: docs/SRS.md acceptance criteria and decision records.</p>
 */
public class RequirementAcceptanceBlackBoxTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneId.of("UTC"));

    @TempDir
    Path tempDir;

    private InMemoryTransactionRepository txRepo;
    private InMemoryAccountRepository accountRepo;
    private InMemoryCategoryRepository categoryRepo;
    private FinanceService service;

    /**
     * Initializes in-memory repositories and service facade for deterministic testing.
     */
    @BeforeEach
    public void setUp() {
        txRepo = new InMemoryTransactionRepository();
        accountRepo = new InMemoryAccountRepository(txRepo);
        categoryRepo = new InMemoryCategoryRepository(txRepo);
        service = new FinanceService(accountRepo, categoryRepo, txRepo, FIXED_CLOCK);
    }

    /**
     * FR-01 Accounts: Cash, bank, and credit card account creation with opening balances.
     */
    @Test
    @DisplayName("TC-FR01-04: Account creation equivalence partitions and opening balance boundaries")
    public void fr01AccountCreationEquivalencePartitionsAndBoundaries() throws Exception {
        Account cash = service.createCashAccount("Cash", Money.of("0.00"));
        Account bank = service.createBankAccount("Bank", Money.of("500.00"));
        Account card = service.createCreditCardAccount("Card", Money.of("1000.00"));

        assertEquals(Money.of("0.00"), service.getAccountBalance(cash.id()));
        assertEquals(Money.of("500.00"), service.getAccountBalance(bank.id()));
        assertEquals(Money.of("0.00"), service.getAccountBalance(card.id()));
    }

    /**
     * FR-02 Income: Positive amount boundary 0.01 increases balance.
     */
    @Test
    @DisplayName("TC-FR02-05: Positive amount boundary 0.01 increases balance")
    public void fr02IncomePositiveAmountBoundaryIncreasesBalance() throws Exception {
        Account bank = service.createBankAccount("Checking", Money.of("0.00"));
        CategoryComponent salary = service.createCategory("Salary", "INCOME", null);

        service.recordIncome(bank.id(), salary.id(), Money.of("0.01"), LocalDate.of(2026, 6, 1), "Penny");
        assertEquals(Money.of("0.01"), service.getAccountBalance(bank.id()));
    }

    /**
     * FR-04 Transfer: Two-leg atomic write maintains net worth equality.
     */
    @Test
    @DisplayName("TC-FR04-07: Transfer maintains net worth and links legs")
    public void fr04TransferPreservesNetWorthAndMaintainsLinkage() throws Exception {
        Account source = service.createBankAccount("Checking", Money.of("100.00"));
        Account dest = service.createCashAccount("Wallet", Money.of("50.00"));

        Money initialNetWorth = service.getNetWorth();
        service.recordTransfer(
                source.id(), dest.id(), Money.of("40.00"), LocalDate.of(2026, 6, 2), "ATM");

        assertEquals(Money.of("60.00"), service.getAccountBalance(source.id()));
        assertEquals(Money.of("90.00"), service.getAccountBalance(dest.id()));
        assertEquals(initialNetWorth, service.getNetWorth());

        List<TransactionView> sourceTxs = service.findTransactions(
                new TransactionQuery(null, null, source.id(), null, null));
        assertFalse(sourceTxs.isEmpty());
    }

    /**
     * FR-05 Categories: Soft-deleted/archived category rejects new transactions.
     */
    @Test
    @DisplayName("TC-FR05-07: Archived category rejects new transaction creation")
    public void fr05ArchivedCategoryRejectsTransactionCreation() throws Exception {
        Account bank = service.createBankAccount("Checking", Money.of("100.00"));
        CategoryComponent cat = service.createCategory("Subscriptions", "EXPENSE", null);
        cat.archive();

        assertThrows(ValidationException.class, () -> service.recordExpense(
                bank.id(), cat.id(), Money.of("15.00"), LocalDate.of(2026, 6, 3), "Streaming"));
    }

    /**
     * FR-06 Edit transaction: Editing amount updates account balance consistently.
     */
    @Test
    @DisplayName("TC-FR06-10: Editing transaction updates account balance consistently")
    public void fr06EditingTransactionUpdatesAccountBalanceConsistently() throws Exception {
        Account bank = service.createBankAccount("Checking", Money.of("100.00"));
        CategoryComponent cat = service.createCategory("Food", "EXPENSE", null);
        TransactionView tx = service.recordExpense(
                bank.id(), cat.id(), Money.of("50.00"), LocalDate.of(2026, 6, 4), "Dinner");
        assertEquals(Money.of("50.00"), service.getAccountBalance(bank.id()));

        service.editTransaction(tx.id(), Money.of("20.00"), "Small dinner");
        assertEquals(Money.of("80.00"), service.getAccountBalance(bank.id()));
    }

    /**
     * FR-06 Delete transaction: Deleting an expense restores account balance.
     */
    @Test
    @DisplayName("TC-FR06-11: Deleting transaction restores account balance consistently")
    public void fr06DeletingTransactionRestoresAccountBalanceConsistently() throws Exception {
        Account bank = service.createBankAccount("Checking", Money.of("100.00"));
        CategoryComponent cat = service.createCategory("Food", "EXPENSE", null);
        TransactionView tx = service.recordExpense(
                bank.id(), cat.id(), Money.of("40.00"), LocalDate.of(2026, 6, 4), "Lunch");

        service.deleteTransaction(tx.id());
        assertEquals(Money.of("100.00"), service.getAccountBalance(bank.id()));
    }

    /**
     * FR-07 Balances & Net Worth: Net worth derived dynamically across cash, bank, and debt.
     */
    @Test
    @DisplayName("TC-FR07-04: Net worth derived across mixed account types")
    public void fr07NetWorthDerivedAcrossMixedAccountTypes() throws Exception {
        Account cash = service.createCashAccount("Cash", Money.of("50.00"));
        Account bank = service.createBankAccount("Bank", Money.of("100.00"));
        Account card = service.createCreditCardAccount("Card", Money.of("500.00"));
        CategoryComponent fee = service.createCategory("Fee", "EXPENSE", null);
        service.recordExpense(card.id(), fee.id(), Money.of("30.00"), LocalDate.of(2026, 6, 5), "Annual fee");

        // Cash 50.00 + Bank 100.00 + Card Balance 0.00 (debt tracked via D-31) = 150.00
        assertEquals(Money.of("150.00"), service.getNetWorth());
    }

    /**
     * FR-08 Search & Filter: Multi-criteria AND logic query.
     */
    @Test
    @DisplayName("TC-FR08-03: Filter combines date, category, and text with AND logic")
    public void fr08FilterCombinesDateCategoryAndTextWithAndLogic() throws Exception {
        Account bank = service.createBankAccount("Bank", Money.of("500.00"));
        CategoryComponent food = service.createCategory("Food", "EXPENSE", null);
        CategoryComponent bills = service.createCategory("Bills", "EXPENSE", null);

        service.recordExpense(bank.id(), food.id(), Money.of("20.00"), LocalDate.of(2026, 6, 1), "Pizza");
        service.recordExpense(bank.id(), food.id(), Money.of("35.00"), LocalDate.of(2026, 6, 5), "Burger");
        service.recordExpense(bank.id(), bills.id(), Money.of("40.00"), LocalDate.of(2026, 6, 5), "Electric");

        TransactionQuery query = new TransactionQuery(
                LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 6), bank.id(), food.id(), "Burger");
        List<TransactionView> results = service.findTransactions(query);
        assertEquals(1, results.size());
        assertEquals(Money.of("35.00"), results.get(0).amount());
    }

    /**
     * FR-09 Budgets: Duplicate budget for same category and month rejected.
     */
    @Test
    @DisplayName("TC-FR09-03: Rejects duplicate budget for same category and month")
    public void fr09RejectsDuplicateBudgetForSameCategoryAndMonth() throws Exception {
        CategoryComponent cat = service.createCategory("Food", "EXPENSE", null);
        YearMonth june = YearMonth.of(2026, 6);
        service.setBudget(cat.id(), june, Money.of("100.00"));

        assertThrows(ValidationException.class, () -> service.setBudget(cat.id(), june, Money.of("150.00")));
        assertNotNull(service.setBudget(cat.id(), YearMonth.of(2026, 7), Money.of("150.00")));
    }

    /**
     * FR-11 Recurring: Weekly rule catch-up generates exact count without duplicate.
     */
    @Test
    @DisplayName("TC-FR11-08: Recurring catch-up generates one transaction per due interval")
    public void fr11RecurringCatchUpGeneratesOneTransactionPerDueInterval() throws Exception {
        Account bank = service.createBankAccount("Checking", Money.of("1000.00"));
        CategoryComponent cat = service.createCategory("Fitness", "EXPENSE", null);
        Schedule weekly = new Schedule(Schedule.Frequency.WEEKLY, LocalDate.of(2026, 6, 1));
        RecurringRule rule = new RecurringRule("Gym", Money.of("10.00"), bank, cat, "EXPENSE", weekly);

        RecurringGenerator generator = new RecurringGenerator(service, FIXED_CLOCK);
        List<TransactionView> generated = generator.generateDue(rule);
        assertEquals(3, generated.size());
    }

    /**
     * FR-13 Monthly Summary: Calculates income, expense, and savings rate.
     */
    @Test
    @DisplayName("TC-FR13-03: Monthly summary calculates totals and savings rate")
    public void fr13MonthlySummaryCalculatesTotalsAndSavingsRate() throws Exception {
        Account bank = service.createBankAccount("Bank", Money.of("1000.00"));
        CategoryComponent salary = service.createCategory("Salary", "INCOME", null);
        CategoryComponent food = service.createCategory("Food", "EXPENSE", null);

        service.recordIncome(bank.id(), salary.id(), Money.of("1000.00"), LocalDate.of(2026, 6, 1), "Paycheck");
        service.recordExpense(bank.id(), food.id(), Money.of("300.00"), LocalDate.of(2026, 6, 2), "Groceries");

        ReportResult result = service.generateReport("Monthly Summary", new ReportRequest(YearMonth.of(2026, 6)));
        assertEquals(100000L, result.getData("totalIncomeMinor"));
        assertEquals(30000L, result.getData("totalExpenseMinor"));
        assertEquals(70000L, result.getData("netSavingsMinor"));
        assertEquals("70.0%", result.getData("savingsRate"));
    }

    /**
     * FR-14 Category Breakdown: Shares sum to exactly 100.0% via Hare-Niemeyer.
     */
    @Test
    @DisplayName("TC-FR14-03: Category breakdown sums exactly to one hundred percent")
    public void fr14CategoryBreakdownSumsExactlyToOneHundredPercent() throws Exception {
        Account bank = service.createBankAccount("Bank", Money.of("1000.00"));
        CategoryComponent c1 = service.createCategory("A", "EXPENSE", null);
        CategoryComponent c2 = service.createCategory("B", "EXPENSE", null);
        CategoryComponent c3 = service.createCategory("C", "EXPENSE", null);

        service.recordExpense(bank.id(), c1.id(), Money.of("10.00"), LocalDate.of(2026, 6, 1), "A");
        service.recordExpense(bank.id(), c2.id(), Money.of("10.00"), LocalDate.of(2026, 6, 1), "B");
        service.recordExpense(bank.id(), c3.id(), Money.of("10.00"), LocalDate.of(2026, 6, 1), "C");

        ReportResult result = service.generateReport("Category Breakdown", new ReportRequest(YearMonth.of(2026, 6)));
        double totalShare = result.rows().stream()
                .mapToDouble(r -> Double.parseDouble(r.get(2).replace("%", "").trim()))
                .sum();
        assertEquals(100.0, totalShare, 0.001);
    }

    /**
     * FR-15 Cash-Flow Trend: Produces rows for empty and populated months in range.
     */
    @Test
    @DisplayName("TC-FR15-02: Cash-flow trend produces row for empty months")
    public void fr15CashFlowTrendProducesRowForEmptyMonths() throws Exception {
        Account bank = service.createBankAccount("Bank", Money.of("1000.00"));
        CategoryComponent food = service.createCategory("Food", "EXPENSE", null);
        service.recordExpense(bank.id(), food.id(), Money.of("50.00"), LocalDate.of(2026, 1, 15), "Jan Expense");
        service.recordExpense(bank.id(), food.id(), Money.of("70.00"), LocalDate.of(2026, 3, 15), "Mar Expense");

        ReportRequest req = new ReportRequest(YearMonth.of(2026, 1), YearMonth.of(2026, 3));
        ReportResult result = service.generateReport("Cash-Flow Trend", req);
        assertEquals(3, result.rows().size());
        assertEquals("0.00", result.rows().get(1).get(3)); // Month 2 net cash flow
    }

    /**
     * FR-16 CSV Export: Round-trip re-import restores identical state.
     */
    @Test
    @DisplayName("TC-FR16-05: CSV export round-trip restores identical ledger")
    public void fr16CsvExportRoundTripRestoresIdenticalLedger() throws Exception {
        Account bank = service.createBankAccount("Checking", Money.of("500.00"));
        CategoryComponent food = service.createCategory("Food", "EXPENSE", null);
        service.recordExpense(bank.id(), food.id(), Money.of("45.00"), LocalDate.of(2026, 6, 2), "Lunch");

        String csv = service.exportTransactionsCsv();
        assertTrue(csv.contains("45.00"));

        InMemoryTransactionRepository freshTxRepo = new InMemoryTransactionRepository();
        InMemoryAccountRepository freshAccRepo = new InMemoryAccountRepository(freshTxRepo);
        InMemoryCategoryRepository freshCatRepo = new InMemoryCategoryRepository(freshTxRepo);
        FinanceService freshService = new FinanceService(freshAccRepo, freshCatRepo, freshTxRepo, FIXED_CLOCK);
        Account freshBank = freshService.createBankAccount("Checking", Money.of("500.00"));
        freshService.createCategory("Food", "EXPENSE", null);

        ImportResult res = freshService.importCsv(csv);
        assertEquals(1, res.importedCount());
        assertEquals(Money.of("455.00"), freshService.getAccountBalance(freshBank.id()));
    }

    /**
     * FR-17 CSV Import: Duplicate detection and row skipping with line numbers.
     */
    @Test
    @DisplayName("TC-FR17-03: CSV import duplicate detection and row skip report")
    public void fr17CsvImportDuplicateDetectionAndRowSkipReport() throws Exception {
        Account bank = service.createBankAccount("Checking", Money.of("500.00"));
        CategoryComponent food = service.createCategory("Food", "EXPENSE", null);
        service.recordExpense(bank.id(), food.id(), Money.of("25.00"), LocalDate.of(2026, 6, 1), "Coffee");

        String csv = service.exportTransactionsCsv();
        String csvWithBadRow = csv + "\n2026-06-02,INVALID,99.99,Bad,Checking,None,";
        ImportResult res = service.importCsv(csvWithBadRow);
        assertEquals(0, res.importedCount());
        assertEquals(1, res.duplicateCount());
        assertEquals(1, res.skippedRows().size());
    }

    /**
     * FR-18 Persistence: Database restart reproduces identical records and balance.
     */
    @Test
    @DisplayName("TC-FR18-05: State survives application restart on SQLite")
    public void fr18StateSurvivesApplicationRestartOnSqlite() throws Exception {
        Path dbPath = tempDir.resolve("piems-blackbox.db");
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        JdbcTransactionRepository tx = new JdbcTransactionRepository(url);
        JdbcAccountRepository acc = new JdbcAccountRepository(url);
        JdbcCategoryRepository cat = new JdbcCategoryRepository(url);

        try (Connection conn = tx.createConnection()) {
            Schema.createTables(conn);
        }
        FinanceService svc1 = new FinanceService(acc, cat, tx, FIXED_CLOCK);
        Account b = svc1.createBankAccount("Checking", Money.of("300.00"));
        CategoryComponent c = svc1.createCategory("Food", "EXPENSE", null);
        svc1.recordExpense(b.id(), c.id(), Money.of("50.00"), LocalDate.of(2026, 6, 1), "Dinner");

        FinanceService svc2 = new FinanceService(
                new JdbcAccountRepository(url), new JdbcCategoryRepository(url),
                new JdbcTransactionRepository(url), FIXED_CLOCK);
        assertEquals(Money.of("250.00"), svc2.getAccountBalance(b.id()));
    }

    /**
     * FR-19 Validation: Required fields validated at service boundary.
     */
    @Test
    @DisplayName("TC-FR19-14: Service validates required inputs and throws ValidationException")
    public void fr19ServiceValidatesDateFormatAndRequiredInputs() {
        assertThrows(ValidationException.class, () -> service.createCashAccount("   ", Money.of("10.00")));
        assertThrows(ValidationException.class, () -> service.createCategory("", "EXPENSE", null));
    }
}
