package piems.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Clock;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.repository.SqliteDatabase;
import piems.service.FinanceService;

/**
 * Production wiring test verifying that application bootstrapping as performed in {@link Main}
 * preserves budgets across application restart without in-memory fallbacks (FR-18, TC-FR18-10, D-46).
 */
class MainBudgetRestartTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("TC-FR18-10: Main production wiring preserves budgets across application restart")
    void mainWiringPreservesBudgetsAcrossRestart() throws Exception {
        Path dbPath = tempDir.resolve("main-wiring-restart.db");
        String dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        YearMonth month = YearMonth.of(2026, 9);

        long categoryId;

        // Startup 1: Wired exactly as Main.main does
        try (SqliteDatabase db = new SqliteDatabase(dbUrl)) {
            FinanceService service = new FinanceService(
                    db.accountRepository(),
                    db.categoryRepository(),
                    db.transactionRepository(),
                    db.budgetRepository(),
                    Clock.systemDefaultZone()
            );
            CategoryComponent books = service.createCategory("Books", "EXPENSE", null);
            categoryId = books.id();
            service.setBudget(categoryId, month, Money.of("350.00"));
            assertThat(service.getBudget(categoryId, month)).isPresent();
        }

        // Startup 2: Reopened exactly as Main.main does on next process start
        try (SqliteDatabase db = new SqliteDatabase(dbUrl)) {
            FinanceService service = new FinanceService(
                    db.accountRepository(),
                    db.categoryRepository(),
                    db.transactionRepository(),
                    db.budgetRepository(),
                    Clock.systemDefaultZone()
            );
            assertThat(service.getBudget(categoryId, month)).isPresent();
            assertThat(service.getBudget(categoryId, month).get().getLimit()).isEqualTo(Money.of("350.00"));
            assertThat(service.listBudgets()).hasSize(1);
        }
    }
}
