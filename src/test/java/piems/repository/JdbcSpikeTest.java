package piems.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import piems.domain.Money;
import piems.domain.PersistenceException;

/**
 * JDBC persistence spike tests verifying SQLite schema, foreign key enforcement,
 * and atomic rollback for two-leg transfers.
 *
 * <p>Verifies TC-FR18-02, TC-FR18-03, TC-FR04-02, TC-FR04-03, and TC-NFR03-01.</p>
 */
class JdbcSpikeTest {

    @TempDir
    Path tempDir;

    private String jdbcUrl;
    private JdbcTransactionRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        Path dbPath = tempDir.resolve("piems-spike.db");
        jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        repository = new JdbcTransactionRepository(jdbcUrl);

        try (Connection conn = repository.createConnection()) {
            Schema.createTables(conn);
        }
    }

    private long createAccount(String name, String type, long openingMinor) throws SQLException {
        String sql = "INSERT INTO accounts (name, type, opening_minor) VALUES (?, ?, ?)";
        try (Connection conn = repository.createConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setLong(3, openingMinor);
            ps.executeUpdate();
            var rs = ps.getGeneratedKeys();
            rs.next();
            return rs.getLong(1);
        }
    }

    private long createCategory(String name, String kind) throws SQLException {
        String sql = "INSERT INTO categories (name, kind) VALUES (?, ?)";
        try (Connection conn = repository.createConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, kind);
            ps.executeUpdate();
            var rs = ps.getGeneratedKeys();
            rs.next();
            return rs.getLong(1);
        }
    }

    @Test
    @DisplayName("TC-FR18-02 / TC-FR02-01: Inserts and reads one transaction with exact Money minor units")
    void insertsAndReadsOneTransaction() throws Exception {
        long accountId = createAccount("Main Checking", "BANK", 100000L);
        long categoryId = createCategory("Groceries", "EXPENSE");

        TransactionRecord record = new TransactionRecord(
                null, "EXPENSE", LocalDate.parse("2026-01-15"),
                Money.ofMinor(2500L), "Supermarket lunch", accountId, null, categoryId, null
        );

        long generatedId = repository.insert(record);
        assertThat(generatedId).isPositive();

        Optional<TransactionRecord> fetched = repository.findById(generatedId);
        assertThat(fetched).isPresent();
        TransactionRecord saved = fetched.get();
        assertThat(saved.id()).isEqualTo(generatedId);
        assertThat(saved.kind()).isEqualTo("EXPENSE");
        assertThat(saved.date()).isEqualTo(LocalDate.parse("2026-01-15"));
        assertThat(saved.amount()).isEqualTo(Money.ofMinor(2500L));
        assertThat(saved.note()).isEqualTo("Supermarket lunch");
        assertThat(saved.accountId()).isEqualTo(accountId);
        assertThat(saved.categoryId()).isEqualTo(categoryId);
    }

    @Test
    @DisplayName("TC-FR18-03: Foreign key PRAGMA rejects transaction referencing non-existent account")
    void rejectsOrphanRowThroughForeignKey() {
        TransactionRecord orphan = new TransactionRecord(
                null, "EXPENSE", LocalDate.parse("2026-01-15"),
                Money.ofMinor(1000L), "Orphan transaction", 9999L, null, null, null
        );

        assertThatThrownBy(() -> repository.insert(orphan))
                .isInstanceOf(PersistenceException.class)
                .hasMessageContaining("Failed to insert transaction")
                .hasCauseInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("TC-NFR03-01 / TC-FR04-02: Failure during multi-update rolls back and leaves nothing stored")
    void failedSecondUpdateLeavesNothingStored() throws Exception {
        long sourceAccountId = createAccount("Source Account", "BANK", 50000L);
        long destAccountId = createAccount("Dest Account", "CASH", 0L);
        String transferLink = UUID.randomUUID().toString();

        TransactionRecord debitLeg = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-01-16"),
                Money.ofMinor(5000L), "Debit leg", sourceAccountId, transferLink, null, null
        );
        TransactionRecord creditLeg = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-01-16"),
                Money.ofMinor(5000L), "Credit leg", destAccountId, transferLink, null, null
        );

        assertThatThrownBy(() -> repository.executeTransferWithFault(debitLeg, creditLeg, true))
                .isInstanceOf(PersistenceException.class)
                .hasMessageContaining("Transfer rolled back");

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("TC-FR04-03: Atomic transfer writes both linked legs or neither (D-23)")
    void transferWritesBothLegsOrNeither() throws Exception {
        long sourceAccountId = createAccount("Checking", "BANK", 50000L);
        long destAccountId = createAccount("Wallet", "CASH", 1000L);
        String transferLink = UUID.randomUUID().toString();

        TransactionRecord debitLeg = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-01-17"),
                Money.ofMinor(2500L), "ATM cash withdrawal", sourceAccountId, transferLink, null, null
        );
        TransactionRecord creditLeg = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-01-17"),
                Money.ofMinor(2500L), "ATM cash deposit", destAccountId, transferLink, null, null
        );

        // Success writes both legs
        repository.executeTransfer(debitLeg, creditLeg);
        assertThat(repository.count()).isEqualTo(2);

        // Failure writes neither leg
        String invalidLink = UUID.randomUUID().toString();
        TransactionRecord badDebit = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-01-17"),
                Money.ofMinor(1000L), "Debit", sourceAccountId, invalidLink, null, null
        );
        TransactionRecord badCreditOrphan = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-01-17"),
                Money.ofMinor(1000L), "Credit to non-existent account", 88888L, invalidLink, null, null
        );

        assertThatThrownBy(() -> repository.executeTransfer(badDebit, badCreditOrphan))
                .isInstanceOf(PersistenceException.class);

        assertThat(repository.count()).isEqualTo(2);
    }
}
