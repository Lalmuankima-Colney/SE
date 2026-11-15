package piems.repository;

import java.nio.file.Path;
import java.sql.Connection;
import org.junit.jupiter.api.io.TempDir;

/**
 * Concrete contract test verifying the JDBC implementation of {@link TransactionRepository}.
 */
class JdbcTransactionRepositoryTest extends TransactionRepositoryContractTest {

    @TempDir
    Path tempDir;

    private String jdbcUrl;

    private void ensureInitialized() throws Exception {
        if (jdbcUrl == null) {
            Path dbPath = tempDir.resolve("piems-tx-" + System.nanoTime() + ".db");
            jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
            JdbcTransactionRepository repo = new JdbcTransactionRepository(jdbcUrl);
            try (Connection conn = repo.createConnection()) {
                Schema.createTables(conn);
            }
        }
    }

    @Override
    protected TransactionRepository createTransactionRepository() throws Exception {
        ensureInitialized();
        return new JdbcTransactionRepository(jdbcUrl);
    }

    @Override
    protected AccountRepository createAccountRepository() throws Exception {
        ensureInitialized();
        return new JdbcAccountRepository(jdbcUrl);
    }

    @Override
    protected CategoryRepository createCategoryRepository() throws Exception {
        ensureInitialized();
        return new JdbcCategoryRepository(jdbcUrl);
    }
}
