package piems.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import piems.domain.Account;
import piems.domain.BankAccount;
import piems.domain.CashAccount;
import piems.domain.Category;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.domain.PersistenceException;

/**
 * Verification of transfer atomicity, rollback on fault injection, and persistence survival
 * across application restarts (Requirements FR-04, FR-18, NFR-03, Decisions D-23, D-25).
 *
 * <p>Verifies TC-FR04-02, TC-NFR03-01, and TC-FR18-01.</p>
 */
class TransferAtomicityTest {

    @TempDir
    Path tempDir;

    private String jdbcUrl;
    private JdbcAccountRepository accountRepository;
    private JdbcTransactionRepository transactionRepository;
    private JdbcCategoryRepository categoryRepository;

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("piems-atomicity.db");
        jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        accountRepository = new JdbcAccountRepository(jdbcUrl);
        transactionRepository = new JdbcTransactionRepository(jdbcUrl);
        categoryRepository = new JdbcCategoryRepository(jdbcUrl);

        try (Connection conn = transactionRepository.createConnection()) {
            Schema.createTables(conn);
        }
    }

    @Test
    @DisplayName("TC-FR04-02 / TC-NFR03-01: Injected transfer fault rolls back and leaves both balances unchanged")
    void failedTransferLeavesBothBalancesUnchanged() throws Exception {
        Account source = accountRepository.save(new BankAccount(null, "Checking Account", Money.of("100.00")));
        Account dest = accountRepository.save(new CashAccount(null, "Physical Cash", Money.of("50.00")));
        String transferLink = UUID.randomUUID().toString();

        TransactionRecord debit = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-03-01"), Money.of("25.00"),
                "ATM Withdrawal", source.id(), transferLink, null, null
        );
        TransactionRecord credit = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-03-01"), Money.of("25.00"),
                "ATM Deposit", dest.id(), transferLink, null, null
        );

        assertThatThrownBy(() -> transactionRepository.executeTransferWithFault(debit, credit, true))
                .isInstanceOf(PersistenceException.class)
                .hasMessageContaining("Transfer rolled back");

        // Both derived balances must remain exactly unchanged (NFR-03, D-25)
        assertThat(accountRepository.getDerivedBalance(source.id())).isEqualTo(Money.of("100.00"));
        assertThat(accountRepository.getDerivedBalance(dest.id())).isEqualTo(Money.of("50.00"));
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("TC-FR04-03: Successful transfer updates both derived balances and preserves net worth")
    void successfulTransferPreservesNetWorthAndUpdatesBalances() throws Exception {
        Account source = accountRepository.save(new BankAccount(null, "Source Bank", Money.of("100.00")));
        Account dest = accountRepository.save(new CashAccount(null, "Wallet Cash", Money.of("50.00")));
        String transferLink = UUID.randomUUID().toString();

        TransactionRecord debit = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-03-02"), Money.of("25.00"),
                "Cash withdrawal", source.id(), transferLink, null, null
        );
        TransactionRecord credit = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-03-02"), Money.of("25.00"),
                "Cash deposit", dest.id(), transferLink, null, null
        );

        transactionRepository.executeTransfer(debit, credit);

        Money sourceBalance = accountRepository.getDerivedBalance(source.id());
        Money destBalance = accountRepository.getDerivedBalance(dest.id());

        assertThat(sourceBalance).isEqualTo(Money.of("75.00"));
        assertThat(destBalance).isEqualTo(Money.of("75.00"));
        assertThat(sourceBalance.add(destBalance)).isEqualTo(Money.of("150.00"));
        assertThat(transactionRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-FR18-01: Application restart reproduces identical data, transactions, and derived balances")
    void stateSurvivesRestart() throws Exception {
        Account bank = accountRepository.save(new BankAccount(null, "Main Checking", Money.of("1000.00")));
        CategoryComponent food = categoryRepository.save(new Category(null, "Groceries", "EXPENSE"));

        transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.parse("2026-03-03"), Money.of("120.50"),
                "Weekly food", bank.id(), null, food.id(), null
        ));

        Money preRestartBalance = accountRepository.getDerivedBalance(bank.id());
        assertThat(preRestartBalance).isEqualTo(Money.of("879.50"));

        // Simulate full application restart with fresh repository instances
        JdbcAccountRepository restartedAccountRepo = new JdbcAccountRepository(jdbcUrl);
        JdbcTransactionRepository restartedTxRepo = new JdbcTransactionRepository(jdbcUrl);
        JdbcCategoryRepository restartedCatRepo = new JdbcCategoryRepository(jdbcUrl);

        assertThat(restartedAccountRepo.count()).isEqualTo(1);
        assertThat(restartedCatRepo.count()).isEqualTo(1);
        assertThat(restartedTxRepo.count()).isEqualTo(1);

        assertThat(restartedAccountRepo.findById(bank.id())).isPresent();
        assertThat(restartedCatRepo.findById(food.id())).isPresent();
        assertThat(restartedAccountRepo.getDerivedBalance(bank.id())).isEqualTo(Money.of("879.50"));
    }
}
