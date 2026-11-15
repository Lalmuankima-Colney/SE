package piems.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Account;
import piems.domain.BankAccount;
import piems.domain.Budget;
import piems.domain.CashAccount;
import piems.domain.Category;
import piems.domain.Money;
import piems.repository.AccountRepository;
import piems.repository.CategoryRepository;
import piems.repository.InMemoryAccountRepository;
import piems.repository.InMemoryCategoryRepository;
import piems.repository.InMemoryTransactionRepository;
import piems.repository.TransactionRecord;
import piems.repository.TransactionRepository;

/**
 * Unit and integration tests for {@link CsvImporter} (FR-17, D-30).
 *
 * <p>Verifies duplicate reconciliation (TC-FR17-01), error reporting with line numbers (TC-FR17-02),
 * and RFC 4180 parsing.</p>
 */
class CsvImporterTest {

    private AccountRepository accountRepo;
    private CategoryRepository categoryRepo;
    private TransactionRepository transactionRepo;
    private BudgetMonitor budgetMonitor;
    private CsvImporter importer;

    @BeforeEach
    void setUp() throws Exception {
        transactionRepo = new InMemoryTransactionRepository();
        accountRepo = new InMemoryAccountRepository(transactionRepo);
        categoryRepo = new InMemoryCategoryRepository();
        Clock clock = Clock.fixed(Instant.parse("2026-01-20T10:00:00Z"), ZoneId.of("UTC"));
        budgetMonitor = new BudgetMonitor(clock);
        importer = new CsvImporter(accountRepo, categoryRepo, transactionRepo, budgetMonitor);

        accountRepo.save(new BankAccount(null, "Bank", Money.of("10000.00")));
        accountRepo.save(new CashAccount(null, "Cash", Money.of("2000.00")));
        categoryRepo.save(new Category(null, "Food", "EXPENSE"));
        categoryRepo.save(new Category(null, "Salary", "INCOME"));
    }

    @Test
    @DisplayName("TC-FR17-01 / D-30: Re-importing identical file adds nothing and reports duplicate counts")
    void reimportAddsNothing() throws Exception {
        String csv = """
                date,kind,account,to_account,category,amount,note
                2026-01-10,INCOME,Bank,,Salary,50000.00,Monthly pay
                2026-01-12,EXPENSE,Bank,,Food,300.00,Lunch
                2026-01-15,TRANSFER,Bank,Cash,,500.00,Cash withdrawal
                """;

        ImportResult firstRun = importer.importCsv(csv);
        assertThat(firstRun.importedCount()).isEqualTo(3);
        assertThat(firstRun.duplicateCount()).isZero();
        assertThat(firstRun.skippedRows()).isEmpty();
        assertThat(transactionRepo.findAll()).hasSize(4); // Income (1) + Expense (1) + Transfer (2 legs) = 4

        ImportResult secondRun = importer.importCsv(csv);
        assertThat(secondRun.importedCount()).isZero();
        assertThat(secondRun.duplicateCount()).isEqualTo(3);
        assertThat(secondRun.skippedRows()).isEmpty();
        assertThat(transactionRepo.findAll()).hasSize(4);
    }

    @Test
    @DisplayName("TC-FR17-02 / D-30: Bad rows and unknown entities are skipped with exact 1-based line numbers")
    void badRowReportsLineNumber() throws Exception {
        String csv = """
                date,kind,account,to_account,category,amount,note
                2026-01-10,INCOME,Bank,,Salary,50000.00,Valid row
                2026-01-11,EXPENSE,NonExistent,,Food,200.00,Unknown account
                2026-01-12,EXPENSE,Bank,,UnknownCategory,200.00,Unknown category
                invalid-date,EXPENSE,Bank,,Food,200.00,Bad date
                2026-01-14,EXPENSE,Bank,,Food,invalid-amount,Bad amount
                2026-01-15,TRANSFER,Bank,Bank,,500.00,Same account transfer
                2026-01-16,EXPENSE,Bank,,Food,150.00,Valid row
                """;

        ImportResult result = importer.importCsv(csv);

        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.duplicateCount()).isZero();
        assertThat(result.skippedRows()).hasSize(5);

        List<Integer> skippedLines = result.skippedRows().stream().map(ImportResult.SkippedRow::lineNumber).toList();
        assertThat(skippedLines).containsExactly(3, 4, 5, 6, 7);

        assertThat(result.skippedRows().get(0).reason()).contains("Unknown account");
        assertThat(result.skippedRows().get(1).reason()).contains("Unknown category");
        assertThat(result.skippedRows().get(2).reason()).contains("Invalid date");
        assertThat(result.skippedRows().get(3).reason()).contains("Invalid amount");
        assertThat(result.skippedRows().get(4).reason()).contains("identical");
    }

    @Test
    @DisplayName("RFC 4180: Quotes, commas, and escaped quotes in notes are preserved faithfully")
    void handlesQuotesAndCommasInNotes() throws Exception {
        String csv = """
                date,kind,account,to_account,category,amount,note
                2026-01-10,EXPENSE,Bank,,Food,250.00,"Weekly ""milk"", eggs & butter"
                """;

        ImportResult result = importer.importCsv(csv);
        assertThat(result.importedCount()).isEqualTo(1);

        List<TransactionRecord> records = transactionRepo.findAll();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).note()).isEqualTo("Weekly \"milk\", eggs & butter");
    }

    @Test
    @DisplayName("Throws ImportException when CSV content is empty or contains only whitespace")
    void emptyCsvThrowsImportException() {
        assertThatThrownBy(() -> importer.importCsv(""))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("empty");

        assertThatThrownBy(() -> importer.importCsv("   \n\t  "))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("Throws ImportException when header columns do not match expected schema")
    void invalidHeaderThrowsImportException() {
        String badHeader = "date,kind,account,amount\n2026-01-10,INCOME,Bank,500.00\n";
        assertThatThrownBy(() -> importer.importCsv(badHeader))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("Invalid CSV header");
    }

    @Test
    @DisplayName("Transfer splits into two atomic linked legs sharing a transferLinkId (D-23)")
    void transferSplitIntoTwoLinkedLegsAtomically() throws Exception {
        String csv = """
                date,kind,account,to_account,category,amount,note
                2026-01-15,TRANSFER,Bank,Cash,,1000.00,Inter-account transfer
                """;

        ImportResult result = importer.importCsv(csv);
        assertThat(result.importedCount()).isEqualTo(1);

        List<TransactionRecord> records = transactionRepo.findAll();
        assertThat(records).hasSize(2);

        TransactionRecord leg1 = records.get(0);
        TransactionRecord leg2 = records.get(1);

        assertThat(leg1.transferLinkId()).isNotNull();
        assertThat(leg1.transferLinkId()).isEqualTo(leg2.transferLinkId());
        assertThat(leg1.amount()).isEqualTo(Money.of("1000.00"));
        assertThat(leg2.amount()).isEqualTo(Money.of("1000.00"));

        Account bank = accountRepo.findById(leg1.accountId()).orElseThrow();
        Account cash = accountRepo.findById(leg2.accountId()).orElseThrow();
        assertThat(bank.name()).isEqualTo("Bank");
        assertThat(cash.name()).isEqualTo("Cash");
    }

    @Test
    @DisplayName("Observer alert triggers when imported expense crosses budget threshold (D-09)")
    void budgetMonitorNotifiedOnExpenseImport() throws Exception {
        var foodCat = categoryRepo.findAll().stream().filter(c -> "Food".equals(c.name())).findFirst().orElseThrow();
        budgetMonitor.registerBudget(new Budget(foodCat, YearMonth.of(2026, 1), Money.of("1000.00")));

        List<BudgetAlertEvent> events = new ArrayList<>();
        budgetMonitor.registerListener(events::add);

        String csv = """
                date,kind,account,to_account,category,amount,note
                2026-01-15,EXPENSE,Bank,,Food,850.00,Big dinner
                """;

        ImportResult result = importer.importCsv(csv);
        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).status().name()).isEqualTo("WARNING");
    }

    @Test
    @DisplayName("TC-FR16-03: Importing expense exceeding account balance is skipped and recorded")
    void importingExpenseExceedingAccountBalanceThrowsImportException() throws Exception {
        String csv = """
                date,kind,account,to_account,category,amount,note
                2026-01-15,EXPENSE,Cash,,Food,999999.00,Way too expensive
                """;
        ImportResult result = importer.importCsv(csv);
        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.skippedRows()).hasSize(1);
        assertThat(result.skippedRows().get(0).reason()).contains("Insufficient funds for expense");
    }

    @Test
    @DisplayName("TC-FR16-04: Importing CSV content starting with UTF-8 BOM succeeds")
    void importingCsvWithUtf8BomSucceeds() throws Exception {
        String csv = "\uFEFFdate,kind,account,to_account,category,amount,note\n"
                + "2026-01-10,INCOME,Cash,,Food,50.00,BOM test income\n";
        ImportResult result = importer.importCsv(csv);
        assertThat(result.importedCount()).isEqualTo(1);
    }
}
