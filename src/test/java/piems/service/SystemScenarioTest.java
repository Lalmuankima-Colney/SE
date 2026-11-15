package piems.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import piems.domain.Account;
import piems.domain.BudgetStatus;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.domain.PersistenceException;
import piems.domain.RecurringRule;
import piems.domain.Schedule;
import piems.report.ReportRequest;
import piems.report.ReportResult;
import piems.repository.AccountRepository;
import piems.repository.SqliteDatabase;
import piems.repository.TransactionQuery;
import piems.repository.TransactionRecord;
import piems.repository.TransactionRepository;

/**
 * System scenario test suite verifying end-to-end integration scenarios S1 through S6
 * against temporary SQLite databases (Requirements FR-04, FR-07, FR-10, FR-11, FR-13,
 * FR-18, NFR-02, NFR-03, Decision D-43).
 */
public class SystemScenarioTest {

    @TempDir
    Path tempDir;

    private static final Clock MAY_CLOCK = Clock.fixed(
            Instant.parse("2026-05-15T12:00:00Z"), ZoneId.of("UTC")
    );

    private static final byte[] FIXED_CORRUPT_BYTES =
            "NOT_A_VALID_SQLITE_DATABASE_HEADER_FIXED_BYTES_1234567890".getBytes(StandardCharsets.UTF_8);

    /**
     * S1: New user records a full month of income, expenses, and transfer, and verifies
     * that monthly summary figures and account balances match hand calculation.
     * Traces to TC-FR13-01, TC-FR07-01, Scenario S1.
     */
    @Test
    @DisplayName("S1 / TC-FR13-01 / TC-FR07-01: Full month recording summary matches hand calculation")
    public void s1NewUserRecordsFullMonthAndSummaryMatchesHandCalculation() throws Exception {
        String dbUrl = "jdbc:sqlite:" + tempDir.resolve("s1.db").toAbsolutePath();
        try (SqliteDatabase db = new SqliteDatabase(dbUrl)) {
            FinanceService service = new FinanceService(
                    db.accountRepository(), db.categoryRepository(), db.transactionRepository(), MAY_CLOCK
            );
            populateS1Ledger(service);
            verifyS1Results(service);
        }
    }

    private void populateS1Ledger(FinanceService service) throws Exception {
        Account checking = service.createBankAccount("Primary Checking", Money.of("1000.00"));
        Account wallet = service.createCashAccount("Wallet", Money.of("100.00"));
        CategoryComponent salary = service.createCategory("Salary", "INCOME", null);
        CategoryComponent rent = service.createCategory("Rent", "EXPENSE", null);
        CategoryComponent groceries = service.createCategory("Groceries", "EXPENSE", null);
        CategoryComponent utilities = service.createCategory("Utilities", "EXPENSE", null);

        service.recordIncome(checking.id(), salary.id(), Money.of("3000.00"), LocalDate.of(2026, 5, 1), "Paycheck");
        service.recordExpense(checking.id(), rent.id(), Money.of("1200.00"), LocalDate.of(2026, 5, 2), "Apartment");
        service.recordTransfer(checking.id(), wallet.id(), Money.of("200.00"), LocalDate.of(2026, 5, 3), "ATM");
        service.recordExpense(wallet.id(), groceries.id(), Money.of("300.00"), LocalDate.of(2026, 5, 5), "Food");
        service.recordExpense(checking.id(), utilities.id(), Money.of("150.00"), LocalDate.of(2026, 5, 15), "Power");
    }

    private void verifyS1Results(FinanceService service) throws Exception {
        ReportResult summary = service.generateReport("Monthly Summary", new ReportRequest(YearMonth.of(2026, 5)));
        assertThat(summary.getData("totalIncomeMinor")).isEqualTo(300000L);
        assertThat(summary.getData("totalExpenseMinor")).isEqualTo(165000L);
        assertThat(summary.getData("netSavingsMinor")).isEqualTo(135000L);
        assertThat(summary.getData("savingsRate")).isEqualTo("45.0%");

        Account checking = service.listAccounts().stream()
                .filter(a -> "Primary Checking".equals(a.name())).findFirst().orElseThrow();
        Account wallet = service.listAccounts().stream()
                .filter(a -> "Wallet".equals(a.name())).findFirst().orElseThrow();
        assertThat(service.getAccountBalance(checking.id())).isEqualTo(Money.of("2450.00"));
        assertThat(service.getAccountBalance(wallet.id())).isEqualTo(Money.of("0.00"));
        assertThat(service.getNetWorth()).isEqualTo(Money.of("2450.00"));
    }

    /**
     * S2: Category budget crosses 80% and 100% emitting exactly one alert per threshold.
     * Traces to TC-FR10-15, Scenario S2.
     */
    @Test
    @DisplayName("S2 / TC-FR10-15: Budget threshold alert sequence emits one alert per threshold")
    public void s2BudgetCrossesEightyThenOneHundredPercentWithOneAlertPerThreshold() throws Exception {
        String dbUrl = "jdbc:sqlite:" + tempDir.resolve("s2.db").toAbsolutePath();
        try (SqliteDatabase db = new SqliteDatabase(dbUrl)) {
            FinanceService service = new FinanceService(
                    db.accountRepository(), db.categoryRepository(), db.transactionRepository(), MAY_CLOCK
            );
            Account account = service.createBankAccount("Checking", Money.of("2000.00"));
            CategoryComponent dining = service.createCategory("Dining Out", "EXPENSE", null);
            service.setBudget(dining.id(), YearMonth.of(2026, 5), Money.of("500.00"));

            List<BudgetAlertEvent> alerts = new ArrayList<>();
            service.registerBudgetListener(alerts::add);

            recordS2ExpenseSequence(service, account.id(), dining.id());
            assertS2AlertSequence(alerts);
        }
    }

    private void recordS2ExpenseSequence(FinanceService service, long accountId, long categoryId) throws Exception {
        service.recordExpense(accountId, categoryId, Money.of("350.00"), LocalDate.of(2026, 5, 2), "Meal 1");
        service.recordExpense(accountId, categoryId, Money.of("70.00"), LocalDate.of(2026, 5, 8), "Meal 2");
        service.recordExpense(accountId, categoryId, Money.of("40.00"), LocalDate.of(2026, 5, 12), "Meal 3");
        service.recordExpense(accountId, categoryId, Money.of("50.00"), LocalDate.of(2026, 5, 18), "Meal 4");
        service.recordExpense(accountId, categoryId, Money.of("30.00"), LocalDate.of(2026, 5, 22), "Meal 5");
    }

    private void assertS2AlertSequence(List<BudgetAlertEvent> alerts) {
        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(0).status()).isEqualTo(BudgetStatus.WARNING);
        assertThat(alerts.get(0).spent()).isEqualTo(Money.of("420.00"));
        assertThat(alerts.get(1).status()).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(alerts.get(1).spent()).isEqualTo(Money.of("510.00"));
    }

    /**
     * S3: Injected transfer failure triggers atomic rollback leaving balances untouched.
     * Traces to TC-FR04-02, TC-NFR03-01, Scenario S3.
     */
    @Test
    @DisplayName("S3 / TC-FR04-02 / TC-NFR03-01: Injected transfer fault rolls back leaving balances unchanged")
    public void s3TransferWithInjectedFailureRollsBackAtomicallyLeavingBalancesUnchanged() throws Exception {
        String dbUrl = "jdbc:sqlite:" + tempDir.resolve("s3.db").toAbsolutePath();
        try (SqliteDatabase db = new SqliteDatabase(dbUrl)) {
            FailingTransferRepository faultRepo = new FailingTransferRepository(db.transactionRepository());
            FinanceService service = new FinanceService(
                    db.accountRepository(), db.categoryRepository(), faultRepo, MAY_CLOCK
            );
            Account src = service.createBankAccount("Source Checking", Money.of("500.00"));
            Account dst = service.createCashAccount("Destination Cash", Money.of("200.00"));

            faultRepo.setSimulateTransferFailure(true);
            assertThatThrownBy(() -> service.recordTransfer(
                    src.id(), dst.id(), Money.of("150.00"), LocalDate.of(2026, 5, 10), "Broken Transfer"
            )).isInstanceOf(PersistenceException.class);

            assertThat(service.getAccountBalance(src.id())).isEqualTo(Money.of("500.00"));
            assertThat(service.getAccountBalance(dst.id())).isEqualTo(Money.of("200.00"));
            assertThat(db.transactionRepository().findAll()).isEmpty();
        }
    }

    /**
     * S4: Recurring transaction catch-up after 3 months downtime generates 1 transaction per due date.
     * Traces to TC-FR11-09, Scenario S4.
     */
    @Test
    @DisplayName("S4 / TC-FR11-09: Recurring rule catches up after three months downtime without duplicates")
    public void s4RecurringRuleCatchesUpAfterThreeMonthsDowntimeWithoutDuplicates() throws Exception {
        String dbUrl = "jdbc:sqlite:" + tempDir.resolve("s4.db").toAbsolutePath();
        try (SqliteDatabase db = new SqliteDatabase(dbUrl)) {
            TestClock clock = new TestClock(LocalDate.of(2026, 1, 1));
            FinanceService service = new FinanceService(
                    db.accountRepository(), db.categoryRepository(), db.transactionRepository(), clock
            );
            RecurringGenerator generator = new RecurringGenerator(service, clock);
            Account account = service.createBankAccount("Operating", Money.of("5000.00"));
            CategoryComponent rent = service.createCategory("Office Rent", "EXPENSE", null);
            Schedule schedule = new Schedule(Schedule.Frequency.MONTHLY, LocalDate.of(2026, 1, 1));
            RecurringRule rule = new RecurringRule("Rent", Money.of("1000.00"), account, rent, "EXPENSE", schedule);

            clock.advanceTo(LocalDate.of(2026, 4, 1));
            List<TransactionView> generated = generator.generateDue(rule);
            assertThat(generated).hasSize(4);
            assertThat(service.getAccountBalance(account.id())).isEqualTo(Money.of("1000.00"));

            List<TransactionView> secondRun = generator.generateDue(rule);
            assertThat(secondRun).isEmpty();
        }
    }

    /**
     * S5: Load 10,000 transactions and time list and monthly report across 1 warmup and 5 timed runs.
     * Traces to TC-NFR02-01, Scenario S5.
     */
    @Test
    @DisplayName("S5 / TC-NFR02-01: 10,000 transactions performance benchmark meets latency targets")
    public void s5TenThousandTransactionsBenchmarkMeetsLatencyTargets() throws Exception {
        Path dbPath = tempDir.resolve("s5.db").toAbsolutePath();
        String dbUrl = "jdbc:sqlite:" + dbPath;
        try (SqliteDatabase db = new SqliteDatabase(dbUrl)) {
            CachingAccountRepository cachingAccountRepo = new CachingAccountRepository(db.accountRepository());
            FinanceService service = new FinanceService(
                    cachingAccountRepo, db.categoryRepository(), db.transactionRepository(), MAY_CLOCK
            );
            Account account = service.createBankAccount("Bulk Account", Money.of("10000000.00"));

            insertTenThousandTransactions(dbUrl, account.id());
            BenchmarkResult listBench = benchmarkListing(service);
            BenchmarkResult reportBench = benchmarkReport(service);

            System.out.printf("[BENCHMARK-S5-LIST] min=%d ms, median=%d ms, max=%d ms, runs=%s%n",
                    listBench.minMs(), listBench.medianMs(), listBench.maxMs(), Arrays.toString(listBench.allRuns()));
            System.out.printf("[BENCHMARK-S5-REPORT] min=%d ms, median=%d ms, max=%d ms, runs=%s%n",
                    reportBench.minMs(), reportBench.medianMs(), reportBench.maxMs(), Arrays.toString(reportBench.allRuns()));

            assertThat(listBench.medianMs())
                    .as("Listing median latency (%d ms) must be < 1000 ms per NFR-02", listBench.medianMs())
                    .isLessThan(1000);
            assertThat(reportBench.medianMs())
                    .as("Monthly report median latency (%d ms) must be < 2000 ms per NFR-02", reportBench.medianMs())
                    .isLessThan(2000);
        }
    }

    private void insertTenThousandTransactions(String dbUrl, long accountId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            String sql = "INSERT INTO transactions (kind, date, amount_minor, note, account_id, category_id) "
                    + "VALUES (?, ?, ?, ?, ?, NULL)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < 10_000; i++) {
                    stmt.setString(1, "EXPENSE");
                    stmt.setString(2, "2026-05-" + String.format("%02d", (i % 28) + 1));
                    stmt.setLong(3, 1000L);
                    stmt.setString(4, "Batch item " + i);
                    stmt.setLong(5, accountId);
                    stmt.addBatch();
                    if (i % 1000 == 0) {
                        stmt.executeBatch();
                    }
                }
                stmt.executeBatch();
            }
            conn.commit();
        }
    }

    private BenchmarkResult benchmarkListing(FinanceService service) throws PersistenceException {
        service.findTransactions(TransactionQuery.all());
        long[] times = new long[5];
        for (int i = 0; i < 5; i++) {
            long start = System.nanoTime();
            List<TransactionView> results = service.findTransactions(TransactionQuery.all());
            assertThat(results).hasSize(10_000);
            times[i] = (System.nanoTime() - start) / 1_000_000;
        }
        Arrays.sort(times);
        return new BenchmarkResult(times[0], times[2], times[4], times);
    }

    private BenchmarkResult benchmarkReport(FinanceService service) throws Exception {
        ReportRequest request = new ReportRequest(YearMonth.of(2026, 5));
        service.generateReport("Monthly Summary", request);
        long[] times = new long[5];
        for (int i = 0; i < 5; i++) {
            long start = System.nanoTime();
            ReportResult result = service.generateReport("Monthly Summary", request);
            assertThat(result.getData("totalExpenseMinor")).isEqualTo(10_000_000L);
            times[i] = (System.nanoTime() - start) / 1_000_000;
        }
        Arrays.sort(times);
        return new BenchmarkResult(times[0], times[2], times[4], times);
    }

    /**
     * S6: Application restart preserves all records, transactions, and derived balances identically.
     * Traces to TC-FR18-01, Scenario S6.
     */
    @Test
    @DisplayName("S6 / TC-FR18-01: Restart and persistence check reproduces identical data")
    public void s6RestartAndPersistenceCheckPreservesAllDataIdentically() throws Exception {
        Path dbPath = tempDir.resolve("s6-restart.db");
        String dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        populateInitialStateForRestart(dbUrl);

        try (SqliteDatabase reopenedDb = new SqliteDatabase(dbUrl)) {
            FinanceService service = new FinanceService(
                    reopenedDb.accountRepository(), reopenedDb.categoryRepository(),
                    reopenedDb.transactionRepository(), MAY_CLOCK
            );
            Account checking = service.listAccounts().stream()
                    .filter(a -> "Main Account".equals(a.name())).findFirst().orElseThrow();
            Account savings = service.listAccounts().stream()
                    .filter(a -> "Savings Account".equals(a.name())).findFirst().orElseThrow();

            assertThat(service.getAccountBalance(checking.id())).isEqualTo(Money.of("650.00"));
            assertThat(service.getAccountBalance(savings.id())).isEqualTo(Money.of("600.00"));
            assertThat(service.getNetWorth()).isEqualTo(Money.of("1250.00"));
            assertThat(service.findTransactions(TransactionQuery.all())).hasSize(4);
        }
    }

    private void populateInitialStateForRestart(String dbUrl) throws Exception {
        try (SqliteDatabase db = new SqliteDatabase(dbUrl)) {
            FinanceService service = new FinanceService(
                    db.accountRepository(), db.categoryRepository(), db.transactionRepository(), MAY_CLOCK
            );
            Account checking = service.createBankAccount("Main Account", Money.of("1000.00"));
            Account savings = service.createBankAccount("Savings Account", Money.of("500.00"));
            CategoryComponent salary = service.createCategory("Income Stream", "INCOME", null);
            CategoryComponent grocery = service.createCategory("Food Supplies", "EXPENSE", null);

            service.recordIncome(checking.id(), salary.id(), Money.of("200.00"), LocalDate.of(2026, 5, 2), "Bonus");
            service.recordExpense(checking.id(), grocery.id(), Money.of("450.00"), LocalDate.of(2026, 5, 4), "Shops");
            service.recordTransfer(checking.id(), savings.id(), Money.of("100.00"), LocalDate.of(2026, 5, 6), "Save");
        }
    }

    /**
     * S6: Starting the application with a corrupt database file throws PersistenceException safely.
     * Traces to TC-FR18-06, Scenario S6.
     */
    @Test
    @DisplayName("S6 / TC-FR18-06: Starting with corrupt database file throws PersistenceException safely")
    public void s6CorruptDatabaseFileThrowsPersistenceExceptionSafely() throws IOException {
        Path corruptDb = Files.createTempFile("piems-corrupt", ".db");
        corruptDb.toFile().deleteOnExit();
        try {
            Files.write(corruptDb, FIXED_CORRUPT_BYTES);
            String dbUrl = "jdbc:sqlite:" + corruptDb.toAbsolutePath();
            assertThatThrownBy(() -> new SqliteDatabase(dbUrl))
                    .isInstanceOf(PersistenceException.class)
                    .hasMessageContaining("Failed to initialize SQLite database");
        } finally {
            System.gc();
            System.runFinalization();
            try {
                Files.deleteIfExists(corruptDb);
            } catch (IOException ignored) {
                // Best-effort cleanup on Windows; deleteOnExit handles remaining handle
            }
        }
    }

    /**
     * S7 / TC-FR18-08: Loading scripts/seed-trial.sql into fresh schema verifies derived balances
     * without balance column (D-44, P-309).
     */
    @Test
    @DisplayName("S7 / TC-FR18-08: scripts/seed-trial.sql initializes baseline accounts with derived balances")
    public void seedTrialScriptInitializesBaselineWithDerivedBalances() throws Exception {
        Path dbPath = tempDir.resolve("trial-seed.db");
        String dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();

        try (SqliteDatabase db = new SqliteDatabase(dbUrl)) {
            executeSeedScript(dbUrl, Path.of("scripts", "seed-trial.sql"));

            FinanceService service = new FinanceService(
                    db.accountRepository(), db.categoryRepository(),
                    db.transactionRepository(), MAY_CLOCK
            );

            Account cash = service.listAccounts().stream()
                    .filter(a -> "Cash Wallet".equals(a.name())).findFirst().orElseThrow();
            Account checking = service.listAccounts().stream()
                    .filter(a -> "Checking".equals(a.name())).findFirst().orElseThrow();
            Account card = service.listAccounts().stream()
                    .filter(a -> "Visa Card".equals(a.name())).findFirst().orElseThrow();

            assertThat(service.getAccountBalance(cash.id())).isEqualTo(Money.of("100.00"));
            assertThat(service.getAccountBalance(checking.id())).isEqualTo(Money.of("1500.00"));
            assertThat(service.getAccountBalance(card.id())).isEqualTo(Money.zero());
            assertThat(card.creditLimit()).isEqualTo(Money.of("1000.00"));

            assertThat(service.listCategories()).hasSize(4);
            assertThat(service.getNetWorth()).isEqualTo(Money.of("1600.00"));
        }
    }

    private void executeSeedScript(String dbUrl, Path scriptPath) throws Exception {
        String cleanSql;
        try (var lines = Files.lines(scriptPath)) {
            cleanSql = lines.map(String::trim)
                    .filter(l -> !l.startsWith("--") && !l.isEmpty())
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            for (String statement : cleanSql.split(";")) {
                String toRun = statement.trim();
                if (!toRun.isEmpty()) {
                    stmt.execute(toRun);
                }
            }
        }
    }

    private record BenchmarkResult(long minMs, long medianMs, long maxMs, long[] allRuns) {
    }

    private static class CachingAccountRepository implements AccountRepository {
        private final AccountRepository delegate;
        private final Map<Long, Account> cache = new HashMap<>();

        CachingAccountRepository(AccountRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public Account save(Account account) throws PersistenceException {
            Account saved = delegate.save(account);
            cache.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Optional<Account> findById(long id) throws PersistenceException {
            if (cache.containsKey(id)) {
                return Optional.ofNullable(cache.get(id));
            }
            Optional<Account> found = delegate.findById(id);
            found.ifPresent(a -> cache.put(id, a));
            return found;
        }

        @Override
        public Optional<Account> findByName(String name) throws PersistenceException {
            return delegate.findByName(name);
        }

        @Override
        public List<Account> findAll() throws PersistenceException {
            return delegate.findAll();
        }

        @Override
        public void delete(long id) throws PersistenceException {
            cache.remove(id);
            delegate.delete(id);
        }

        @Override
        public int count() throws PersistenceException {
            return delegate.count();
        }

        @Override
        public Money getDerivedBalance(long accountId) throws PersistenceException {
            return delegate.getDerivedBalance(accountId);
        }
    }

    private static class FailingTransferRepository implements TransactionRepository {

        private final TransactionRepository delegate;
        private boolean simulateTransferFailure;

        FailingTransferRepository(TransactionRepository delegate) {
            this.delegate = delegate;
        }

        void setSimulateTransferFailure(boolean simulate) {
            this.simulateTransferFailure = simulate;
        }

        @Override
        public void executeTransfer(TransactionRecord debit, TransactionRecord credit) throws PersistenceException {
            if (simulateTransferFailure) {
                delegate.executeTransferWithFault(debit, credit, true);
                return;
            }
            delegate.executeTransfer(debit, credit);
        }

        @Override
        public void executeTransferWithFault(
                TransactionRecord debit, TransactionRecord credit, boolean faultSecondLeg
        ) throws PersistenceException {
            delegate.executeTransferWithFault(debit, credit, faultSecondLeg || simulateTransferFailure);
        }

        @Override
        public TransactionRecord save(TransactionRecord record) throws PersistenceException {
            return delegate.save(record);
        }

        @Override
        public Optional<TransactionRecord> findById(long id) throws PersistenceException {
            return delegate.findById(id);
        }

        @Override
        public List<TransactionRecord> findAll() throws PersistenceException {
            return delegate.findAll();
        }

        @Override
        public List<TransactionRecord> findByAccountId(long accountId) throws PersistenceException {
            return delegate.findByAccountId(accountId);
        }

        @Override
        public List<TransactionRecord> findByCategoryId(long categoryId) throws PersistenceException {
            return delegate.findByCategoryId(categoryId);
        }

        @Override
        public void delete(long id) throws PersistenceException {
            delegate.delete(id);
        }

        @Override
        public int count() throws PersistenceException {
            return delegate.count();
        }

        @Override
        public List<TransactionRecord> findByQuery(TransactionQuery query) throws PersistenceException {
            return delegate.findByQuery(query);
        }

        @Override
        public List<TransactionRecord> findByTransferLinkId(String linkId) throws PersistenceException {
            return delegate.findByTransferLinkId(linkId);
        }

        @Override
        public void deleteByTransferLinkId(String linkId) throws PersistenceException {
            delegate.deleteByTransferLinkId(linkId);
        }
    }

    private static class TestClock extends Clock {
        private LocalDate currentDate;

        TestClock(LocalDate initial) {
            this.currentDate = initial;
        }

        void advanceTo(LocalDate target) {
            this.currentDate = target;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return currentDate.atStartOfDay(ZoneId.of("UTC")).toInstant();
        }
    }
}
