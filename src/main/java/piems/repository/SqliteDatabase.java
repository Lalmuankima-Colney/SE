package piems.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import piems.domain.PersistenceException;

/**
 * SQLite database manager handling connection lifecycle and schema bootstrap.
 *
 * <p>Encapsulates all SQL driver interactions so that {@code java.sql.*} imports
 * remain strictly confined to the {@code piems.repository} package per Decision D-05,
 * D-11, and Requirement FR-18.</p>
 */
public class SqliteDatabase implements AutoCloseable {

    private final Connection connection;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Opens a connection to the specified SQLite database and initializes tables.
     *
     * @param jdbcUrl JDBC connection URL (e.g., "jdbc:sqlite:piems.db")
     * @throws PersistenceException if connection or schema creation fails
     */
    public SqliteDatabase(String jdbcUrl) throws PersistenceException {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl cannot be null");
        try {
            this.connection = DriverManager.getConnection(jdbcUrl);
            enableForeignKeys(this.connection);
            Schema.createTables(this.connection);
            this.accountRepository = new JdbcAccountRepository(jdbcUrl);
            this.categoryRepository = new JdbcCategoryRepository(jdbcUrl);
            this.transactionRepository = new JdbcTransactionRepository(jdbcUrl);
        } catch (SQLException e) {
            throw new PersistenceException("Failed to initialize SQLite database: " + e.getMessage(), e);
        }
    }

    private static void enableForeignKeys(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
    }

    /**
     * Returns the account repository instance.
     *
     * @return account repository
     */
    public AccountRepository accountRepository() {
        return accountRepository;
    }

    /**
     * Returns the category repository instance.
     *
     * @return category repository
     */
    public CategoryRepository categoryRepository() {
        return categoryRepository;
    }

    /**
     * Returns the transaction repository instance.
     *
     * @return transaction repository
     */
    public TransactionRepository transactionRepository() {
        return transactionRepository;
    }

    @Override
    public void close() throws PersistenceException {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to close SQLite connection: " + e.getMessage(), e);
        }
    }
}
