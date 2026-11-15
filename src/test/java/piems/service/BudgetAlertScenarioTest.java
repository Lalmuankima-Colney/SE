package piems.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Account;
import piems.domain.Budget;
import piems.domain.BudgetStatus;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.domain.PiemsException;
import piems.repository.InMemoryAccountRepository;
import piems.repository.InMemoryCategoryRepository;
import piems.repository.InMemoryTransactionRepository;

/**
 * System integration test for Scenario S2: Budget threshold alerts via Observer pattern
 * (FR-10, D-09, D-28, Scenario S2).
 */
class BudgetAlertScenarioTest {

    private FinanceService service;
    private Account account;
    private CategoryComponent diningCategory;
    private YearMonth may2026;

    @BeforeEach
    void setUp() throws PiemsException {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-15T12:00:00Z"), ZoneId.of("UTC"));
        InMemoryTransactionRepository txRepo = new InMemoryTransactionRepository();
        InMemoryAccountRepository accountRepo = new InMemoryAccountRepository(txRepo);
        InMemoryCategoryRepository catRepo = new InMemoryCategoryRepository();

        service = new FinanceService(accountRepo, catRepo, txRepo, fixedClock);
        account = service.createBankAccount("Checking", Money.of("1000.00"));
        diningCategory = service.createCategory("Dining Out", "EXPENSE", null);
        may2026 = YearMonth.of(2026, 5);
    }

    @Test
    @DisplayName("Scenario S2: Budget crosses 80% then 100% emitting exactly one alert per threshold")
    void budgetAlertScenarioS2CrossesEightyThenOneHundredPercent() throws PiemsException {
        List<BudgetAlertEvent> alerts = new ArrayList<>();
        service.registerBudgetListener(alerts::add);
        service.setBudget(diningCategory.id(), may2026, Money.of("100.00"));

        service.recordExpense(account.id(), diningCategory.id(), Money.of("50.00"), LocalDate.of(2026, 5, 2), "Lunch");
        assertThat(alerts).isEmpty();

        service.recordExpense(account.id(), diningCategory.id(), Money.of("30.00"), LocalDate.of(2026, 5, 8), "Dinner");
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).status()).isEqualTo(BudgetStatus.WARNING);
        assertThat(alerts.get(0).spent()).isEqualTo(Money.of("80.00"));

        service.recordExpense(account.id(), diningCategory.id(), Money.of("10.00"), LocalDate.of(2026, 5, 12), "Snack");
        assertThat(alerts).hasSize(1);

        service.recordExpense(account.id(), diningCategory.id(), Money.of("10.00"), LocalDate.of(2026, 5, 18), "Coffee");
        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(1).status()).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(alerts.get(1).spent()).isEqualTo(Money.of("100.00"));

        service.recordExpense(account.id(), diningCategory.id(), Money.of("20.00"), LocalDate.of(2026, 5, 22), "Pizza");
        assertThat(alerts).hasSize(2);
        assertThat(service.getAccountBalance(account.id())).isEqualTo(Money.of("880.00"));
    }

    @Test
    @DisplayName("D-09: Throwing listener does not abort transaction recording or disrupt sibling listeners")
    void throwingListenerDoesNotBreakExpenseRecording() throws PiemsException {
        List<BudgetAlertEvent> recordedAlerts = new ArrayList<>();
        service.registerBudgetListener(event -> {
            throw new IllegalStateException("Faulty listener explosion");
        });
        service.registerBudgetListener(recordedAlerts::add);
        service.setBudget(diningCategory.id(), may2026, Money.of("100.00"));

        assertThatCode(() -> service.recordExpense(
                account.id(), diningCategory.id(), Money.of("80.00"), LocalDate.of(2026, 5, 5), "Dinner"
        )).doesNotThrowAnyException();

        assertThat(recordedAlerts).hasSize(1);
        assertThat(recordedAlerts.get(0).status()).isEqualTo(BudgetStatus.WARNING);
        assertThat(service.getAccountBalance(account.id())).isEqualTo(Money.of("920.00"));
    }

    @Test
    @DisplayName("TC-FR10-06 / D-28: Threshold re-crossing alerts again after status reset")
    void budgetReCrossingWithFinanceService() throws PiemsException {
        List<BudgetAlertEvent> alerts = new ArrayList<>();
        service.registerBudgetListener(alerts::add);
        Budget budget = service.setBudget(diningCategory.id(), may2026, Money.of("100.00"));

        service.getBudgetMonitor().check(budget, Money.of("80.00"), LocalDate.of(2026, 5, 5));
        assertThat(alerts).hasSize(1);

        service.getBudgetMonitor().check(budget, Money.of("60.00"), LocalDate.of(2026, 5, 6));
        assertThat(service.getBudgetMonitor().getLastNotifiedStatus(diningCategory, may2026))
                .isEqualTo(BudgetStatus.OK);

        service.getBudgetMonitor().check(budget, Money.of("80.00"), LocalDate.of(2026, 5, 7));
        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(1).status()).isEqualTo(BudgetStatus.WARNING);
    }
}
