package piems.repository;

import java.nio.file.Path;
import java.sql.Connection;
import org.junit.jupiter.api.io.TempDir;

/**
 * Concrete contract test verifying the JDBC implementation of {@link CategoryRepository}.
 */
class JdbcCategoryRepositoryTest extends CategoryRepositoryContractTest {

    @TempDir
    Path tempDir;

    @Override
    protected CategoryRepository createRepository() throws Exception {
        Path dbPath = tempDir.resolve("piems-categories-" + System.nanoTime() + ".db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        JdbcCategoryRepository repo = new JdbcCategoryRepository(jdbcUrl);
        try (Connection conn = repo.createConnection()) {
            Schema.createTables(conn);
        }
        return repo;
    }
}
