package piems.repository;

import java.nio.file.Path;
import java.sql.Connection;
import org.junit.jupiter.api.io.TempDir;

/**
 * Concrete contract test verifying the JDBC implementation of {@link AccountRepository}.
 */
class JdbcAccountRepositoryTest extends AccountRepositoryContractTest {

    @TempDir
    Path tempDir;

    @Override
    protected AccountRepository createRepository() throws Exception {
        Path dbPath = tempDir.resolve("piems-accounts-" + System.nanoTime() + ".db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        JdbcAccountRepository repo = new JdbcAccountRepository(jdbcUrl);
        try (Connection conn = repo.createConnection()) {
            Schema.createTables(conn);
        }
        return repo;
    }
}
