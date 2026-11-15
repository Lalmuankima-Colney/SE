package piems.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Account;
import piems.domain.BankAccount;
import piems.domain.CashAccount;
import piems.domain.Category;
import piems.domain.Money;
import piems.repository.AccountRepository;
import piems.repository.CategoryRepository;
import piems.repository.InMemoryAccountRepository;
import piems.repository.InMemoryCategoryRepository;
import piems.repository.InMemoryTransactionRepository;
import piems.repository.TransactionRepository;

/**
 * Integration round-trip test verifying that exporting to CSV and importing into
 * an empty ledger restores transactions, balances, and transfer linkages identically (FR-16, FR-17, D-23, D-30).
 *
 * <p>Traces to TC-FR16-01, TC-FR16-02, and TC-FR16-06.</p>
 */
class CsvRoundTripTest {

    private FinanceService sourceService;
    private FinanceService targetService;
    private AccountRepository sourceAccountRepo;
    private AccountRepository targetAccountRepo;
    private CategoryRepository sourceCategoryRepo;
    private CategoryRepository targetCategoryRepo;

    @BeforeEach
    void setUp() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-01-20T10:00:00Z"), ZoneId.of("UTC"));

        TransactionRepository sourceTxRepo = new InMemoryTransactionRepository();
        sourceAccountRepo = new InMemoryAccountRepository(sourceTxRepo);
        sourceCategoryRepo = new InMemoryCategoryRepository();
        sourceService = new FinanceService(sourceAccountRepo, sourceCategoryRepo, sourceTxRepo, clock);

        TransactionRepository targetTxRepo = new InMemoryTransactionRepository();
        targetAccountRepo = new InMemoryAccountRepository(targetTxRepo);
        targetCategoryRepo = new InMemoryCategoryRepository();
        targetService = new FinanceService(targetAccountRepo, targetCategoryRepo, targetTxRepo, clock);

        // Populate identical initial accounts and categories in both ledgers
        sourceAccountRepo.save(new BankAccount(null, "Bank", Money.of("20000.00")));
        sourceAccountRepo.save(new CashAccount(null, "Cash", Money.of("1000.00")));
        sourceCategoryRepo.save(new Category(null, "Salary", "INCOME"));
        sourceCategoryRepo.save(new Category(null, "Food", "EXPENSE"));

        targetAccountRepo.save(new BankAccount(null, "Bank", Money.of("20000.00")));
        targetAccountRepo.save(new CashAccount(null, "Cash", Money.of("1000.00")));
        targetCategoryRepo.save(new Category(null, "Salary", "INCOME"));
        targetCategoryRepo.save(new Category(null, "Food", "EXPENSE"));
    }

    @Test
    @DisplayName("TC-FR16-02 / D-30: Ledger with transfer exports single CSV row with kind=TRANSFER and to_account")
    void singleCsvRowWithTransferExported() throws Exception {
        Account bank = findAccount(sourceAccountRepo, "Bank");
        Account cash = findAccount(sourceAccountRepo, "Cash");

        sourceService.recordTransfer(
                bank.id(), cash.id(), Money.of("1500.00"),
                LocalDate.of(2026, 1, 16), "ATM withdrawal"
        );

        String csv = sourceService.exportTransactionsCsv();

        assertThat(csv)
                .startsWith("date,kind,account,to_account,category,amount,note\n")
                .contains("2026-01-16,TRANSFER,Bank,Cash,,1500.00,ATM withdrawal\n");

        List<String> lines = csv.lines().filter(line -> !line.isBlank()).toList();
        // Header + exactly 1 data row for the transfer (deduplicated across the two legs)
        assertThat(lines).hasSize(2);
    }

    @Test
    @DisplayName("TC-FR16-01 / D-30: Round-trip export then import into an empty ledger reproduces identical state")
    void exportThenImportIsIdentical() throws Exception {
        Account bank = findAccount(sourceAccountRepo, "Bank");
        Account cash = findAccount(sourceAccountRepo, "Cash");
        long salaryCatId = sourceCategoryRepo.findAll().stream()
                .filter(c -> "Salary".equals(c.name())).findFirst().orElseThrow().id();
        long foodCatId = sourceCategoryRepo.findAll().stream()
                .filter(c -> "Food".equals(c.name())).findFirst().orElseThrow().id();

        sourceService.recordIncome(bank.id(), salaryCatId, Money.of("50000.00"), LocalDate.of(2026, 1, 10), "Monthly Pay");
        sourceService.recordExpense(bank.id(), foodCatId, Money.of("250.00"), LocalDate.of(2026, 1, 12), "Groceries");
        sourceService.recordTransfer(bank.id(), cash.id(), Money.of("500.00"), LocalDate.of(2026, 1, 15), "ATM cash");

        String exportedCsv = sourceService.exportTransactionsCsv();

        // Import into empty target ledger
        ImportResult importResult = targetService.importCsv(exportedCsv);
        assertThat(importResult.importedCount()).isEqualTo(3);
        assertThat(importResult.duplicateCount()).isZero();
        assertThat(importResult.skippedRows()).isEmpty();

        // Verify account balances match in target ledger
        Account targetBank = findAccount(targetAccountRepo, "Bank");
        Account targetCash = findAccount(targetAccountRepo, "Cash");

        Money sourceBankBalance = sourceAccountRepo.getDerivedBalance(bank.id());
        Money sourceCashBalance = sourceAccountRepo.getDerivedBalance(cash.id());
        Money targetBankBalance = targetAccountRepo.getDerivedBalance(targetBank.id());
        Money targetCashBalance = targetAccountRepo.getDerivedBalance(targetCash.id());

        assertThat(targetBankBalance).isEqualTo(sourceBankBalance);
        assertThat(targetCashBalance).isEqualTo(sourceCashBalance);

        // Verify re-export from target matches source export identically
        String targetExportedCsv = targetService.exportTransactionsCsv();
        assertThat(targetExportedCsv).isEqualTo(exportedCsv);
    }

    @Test
    @DisplayName("TC-FR16-06 / D-44: Formula triggers (=, +, -, @, ') neutralized in export and restored losslessly on import")
    void formulaTriggerCharactersNeutralizedAndRestoredLosslessly() throws Exception {
        Account bank = findAccount(sourceAccountRepo, "Bank");
        long catId = sourceCategoryRepo.findAll().stream()
                .filter(c -> "Food".equals(c.name())).findFirst().orElseThrow().id();
        List<String> originalNotes = List.of(
                "=SUM(A1:B10) formula", "+12345 addition", "-50 discount",
                "@user mention", "'single quote note", "'"
        );
        for (int i = 0; i < originalNotes.size(); i++) {
            sourceService.recordExpense(bank.id(), catId, Money.ofMinor((i + 1) * 1000),
                    LocalDate.of(2026, 1, 10 + i), originalNotes.get(i));
        }

        String exportedCsv = sourceService.exportTransactionsCsv();
        assertExportNeutralized(exportedCsv);

        ImportResult importResult = targetService.importCsv(exportedCsv);
        assertThat(importResult.importedCount()).isEqualTo(6);
        assertThat(importResult.skippedRows()).isEmpty();

        List<String> importedNotes = targetService.findTransactions(
                new piems.repository.TransactionQuery(null, null, null, null, null)
        ).stream().map(TransactionView::note).toList();
        assertThat(importedNotes).containsExactlyInAnyOrderElementsOf(originalNotes);
    }

    private void assertExportNeutralized(String csv) {
        assertThat(csv)
                .contains("'=SUM(A1:B10) formula")
                .contains("'+12345 addition")
                .contains("'-50 discount")
                .contains("'@user mention")
                .contains("''single quote note")
                .contains(",''\n")
                .doesNotContain("'2026")
                .doesNotContain("'10.00");
    }

    private Account findAccount(AccountRepository repo, String name) throws Exception {
        return repo.findAll().stream()
                .filter(a -> a.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow();
    }
}
