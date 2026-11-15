package piems.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import piems.domain.Account;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.service.FinanceService;

/**
 * Fail-first proof test verifying budget persistence across application restart (FR-18, TC-FR18-09).
 */
class RestartPersistenceCheckTest {

    @TempDir
    Path tempDir;

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-15T10:00:00Z"),
            ZoneId.of("UTC")
    );

    @Test
    @DisplayName("TC-FR18-09: Budget survives database close and restart with fresh service instances")
    void testBudgetPersistsAcrossRestart() throws Exception {
        Path dbPath = tempDir.resolve("restart-check.db");
        String dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        YearMonth month = YearMonth.of(2026, 5);

        long catId;
        try (SqliteDatabase db = new SqliteDatabase(dbUrl)) {
            FinanceService service1 = new FinanceService(
                    db.accountRepository(),
                    db.categoryRepository(),
                    db.transactionRepository(),
                    db.budgetRepository(),
                    FIXED_CLOCK
            );
            Account account = service1.createBankAccount("Checking", Money.of("1000.00"));
            CategoryComponent category = service1.createCategory("Food", "EXPENSE", null);
            catId = category.id();
            service1.setBudget(catId, month, Money.of("500.00"));
            assertThat(service1.getBudget(catId, month)).isPresent();
        }

        // Reopen database with fresh service instances (simulating restart)
        try (SqliteDatabase db = new SqliteDatabase(dbUrl)) {
            FinanceService service2 = new FinanceService(
                    db.accountRepository(),
                    db.categoryRepository(),
                    db.transactionRepository(),
                    db.budgetRepository(),
                    FIXED_CLOCK
            );
            assertThat(service2.getBudget(catId, month)).isPresent();
            assertThat(service2.getBudget(catId, month).get().getLimit()).isEqualTo(Money.of("500.00"));
        }
    }
}
