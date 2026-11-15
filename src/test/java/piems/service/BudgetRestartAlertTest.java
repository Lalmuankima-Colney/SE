package piems.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import piems.domain.Account;
import piems.domain.BudgetStatus;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.repository.SqliteDatabase;

/**
 * Verifies budget preloading and hysteresis re-arming across application restart (FR-10, FR-18, D-28, D-46).
 */
class BudgetRestartAlertTest {

    @TempDir
    Path tempDir;

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-15T10:00:00Z"),
            ZoneId.of("UTC")
    );

    @Test
    @DisplayName("TC-FR10-16: Alert repeats after restart due to session-scoped hysteresis reset to OK")
    void alertRepeatsAfterRestartWhenSpendingRemainsOverThreshold() throws Exception {
        Path dbPath = tempDir.resolve("restart-alert.db");
        String dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        YearMonth june = YearMonth.of(2026, 6);

        long accountId;
        long categoryId;
        List<BudgetAlertEvent> session1Alerts = new ArrayList<>();

        // Session 1: Setup and cross 80% threshold
        try (SqliteDatabase db1 = new SqliteDatabase(dbUrl)) {
            FinanceService service1 = new FinanceService(
                    db1.accountRepository(),
                    db1.categoryRepository(),
                    db1.transactionRepository(),
                    db1.budgetRepository(),
                    FIXED_CLOCK
            );
            service1.registerBudgetListener(session1Alerts::add);

            Account bank = service1.createBankAccount("Main Checking", Money.of("1000.00"));
            accountId = bank.id();
            CategoryComponent dining = service1.createCategory("Dining", "EXPENSE", null);
            categoryId = dining.id();

            service1.setBudget(categoryId, june, Money.of("100.00"));

            // 85.00 spent crosses 80% threshold -> WARNING alert
            service1.recordExpense(accountId, categoryId, Money.of("85.00"), LocalDate.of(2026, 6, 2), "Dinner");
            assertThat(session1Alerts).hasSize(1);
            assertThat(session1Alerts.get(0).status()).isEqualTo(BudgetStatus.WARNING);

            // Additional 5.00 spent within same session -> No duplicate alert
            service1.recordExpense(accountId, categoryId, Money.of("5.00"), LocalDate.of(2026, 6, 3), "Coffee");
            assertThat(session1Alerts).hasSize(1);
        }

        List<BudgetAlertEvent> session2Alerts = new ArrayList<>();

        // Session 2: Application restarted with fresh service and monitor
        try (SqliteDatabase db2 = new SqliteDatabase(dbUrl)) {
            FinanceService service2 = new FinanceService(
                    db2.accountRepository(),
                    db2.categoryRepository(),
                    db2.transactionRepository(),
                    db2.budgetRepository(),
                    FIXED_CLOCK
            );
            service2.registerBudgetListener(session2Alerts::add);

            // Preload verified: budget exists in session 2
            assertThat(service2.getBudget(categoryId, june)).isPresent();
            assertThat(service2.getBudgetMonitor().getLastNotifiedStatus(
                    service2.getCategory(categoryId), june)).isEqualTo(BudgetStatus.OK);

            // New expense in session 2: evaluated spending is 92.00 (92% >= 80%)
            // Under D-46, session hysteresis starts at OK, so WARNING alert fires again in new session
            service2.recordExpense(accountId, categoryId, Money.of("2.00"), LocalDate.of(2026, 6, 4), "Snack");
            assertThat(session2Alerts).hasSize(1);
            assertThat(session2Alerts.get(0).status()).isEqualTo(BudgetStatus.WARNING);
        }
    }
}
