package piems.repository;

import java.nio.file.Path;
import java.sql.Connection;
import org.junit.jupiter.api.io.TempDir;

/**
 * Concrete contract test verifying {@link JdbcBudgetRepository} (D-11, D-46).
 */
class JdbcBudgetRepositoryTest extends BudgetRepositoryContractTest {

    @TempDir
    Path tempDir;

    private String jdbcUrl;

    @Override
    protected CategoryRepository createCategoryRepository() throws Exception {
        Path dbPath = tempDir.resolve("piems-budget-" + System.nanoTime() + ".db");
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        JdbcCategoryRepository catRepo = new JdbcCategoryRepository(jdbcUrl);
        try (Connection conn = catRepo.createConnection()) {
            Schema.createTables(conn);
        }
        return catRepo;
    }

    @Override
    protected BudgetRepository createBudgetRepository(CategoryRepository categoryRepository) {
        return new JdbcBudgetRepository(jdbcUrl, categoryRepository);
    }
}
