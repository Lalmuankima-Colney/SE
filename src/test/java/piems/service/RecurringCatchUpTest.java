package piems.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Account;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.domain.RecurringRule;
import piems.domain.Schedule;
import piems.repository.InMemoryAccountRepository;
import piems.repository.InMemoryCategoryRepository;
import piems.repository.InMemoryTransactionRepository;

/**
 * Scenario S4 and catch-up test suite for {@link RecurringGenerator} (FR-11, TC-FR11-01, TC-FR11-02, D-14).
 */
class RecurringCatchUpTest {

    private TestClock clock;
    private FinanceService financeService;
    private RecurringGenerator generator;
    private Account checkingAccount;
    private CategoryComponent rentCategory;

    @BeforeEach
    void setUp() throws Exception {
        clock = new TestClock(LocalDate.of(2026, 1, 1));
        InMemoryTransactionRepository transactionRepo = new InMemoryTransactionRepository();
        InMemoryAccountRepository accountRepo = new InMemoryAccountRepository(transactionRepo);
        InMemoryCategoryRepository categoryRepo = new InMemoryCategoryRepository();

        financeService = new FinanceService(accountRepo, categoryRepo, transactionRepo, clock);
        generator = new RecurringGenerator(financeService, clock);

        checkingAccount = financeService.createBankAccount("Checking", Money.of("5000.00"));
        rentCategory = financeService.createCategory("Housing Rent", "EXPENSE", null);
    }

    @Test
    @DisplayName("TC-FR11-01 Scenario S4: Generator catches up after 3 months downtime with 1 transaction per due date")
    void generatesOneTransactionPerDueDateAfterDowntime() throws Exception {
        Schedule monthlyRentSchedule = new Schedule(Schedule.Frequency.MONTHLY, LocalDate.of(2026, 1, 1));
        RecurringRule rentRule = new RecurringRule(
                "Monthly Rent", Money.of("1000.00"), checkingAccount, rentCategory, "EXPENSE", monthlyRentSchedule
        );

        // Advance clock forward by three months of downtime to April 1st
        clock.advanceTo(LocalDate.of(2026, 4, 1));

        List<TransactionView> generated = generator.generateDue(rentRule);

        // Expect exactly 4 transactions: Jan 1, Feb 1, Mar 1, Apr 1
        assertThat(generated).hasSize(4);
        assertThat(generated).extracting(TransactionView::date).containsExactly(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1)
        );
        assertThat(generated).extracting(TransactionView::amount)
                .containsOnly(Money.of("1000.00"));

        // Account balance reduced by 4000.00 (from 5000.00 to 1000.00)
        Money derivedBalance = financeService.getAccountBalance(checkingAccount.id());
        assertThat(derivedBalance).isEqualTo(Money.of("1000.00"));
    }

    @Test
    @DisplayName("TC-FR11-02 Second run adds nothing: Executing generator again on same date produces 0 transactions")
    void secondRunAddsNothing() throws Exception {
        Schedule monthlySchedule = new Schedule(Schedule.Frequency.MONTHLY, LocalDate.of(2026, 1, 1));
        RecurringRule salaryRule = new RecurringRule(
                "Monthly Salary", Money.of("3000.00"), checkingAccount, null, "INCOME", monthlySchedule
        );

        clock.advanceTo(LocalDate.of(2026, 3, 1));

        List<TransactionView> firstRun = generator.generateDue(salaryRule);
        assertThat(firstRun).hasSize(3);

        // Second run on the exact same day
        List<TransactionView> secondRun = generator.generateDue(salaryRule);
        assertThat(secondRun).isEmpty();

        // Account balance reflects only the 3 generated transactions (5000 + 3*3000 = 14000)
        Money derivedBalance = financeService.getAccountBalance(checkingAccount.id());
        assertThat(derivedBalance).isEqualTo(Money.of("14000.00"));
    }

    @Test
    @DisplayName("TC-FR11-01 Batch processing: generateAll processes multiple distinct recurring rules")
    void batchProcessingExecutesMultipleRules() throws Exception {
        Schedule rentSchedule = new Schedule(Schedule.Frequency.MONTHLY, LocalDate.of(2026, 1, 1));
        RecurringRule rentRule = new RecurringRule(
                "Rent", Money.of("500.00"), checkingAccount, rentCategory, "EXPENSE", rentSchedule
        );

        Schedule subSchedule = new Schedule(Schedule.Frequency.MONTHLY, LocalDate.of(2026, 1, 15));
        RecurringRule subRule = new RecurringRule(
                "Streaming", Money.of("15.00"), checkingAccount, null, "EXPENSE", subSchedule
        );

        clock.advanceTo(LocalDate.of(2026, 2, 20));
        List<TransactionView> allGenerated = generator.generateAll(List.of(rentRule, subRule));

        // Rent: Jan 1, Feb 1 (2 tx); Streaming: Jan 15, Feb 15 (2 tx) -> Total 4 tx
        assertThat(allGenerated).hasSize(4);
    }

    private static final class TestClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zone;

        TestClock(LocalDate initialDate) {
            this.zone = ZoneId.of("UTC");
            this.currentInstant = initialDate.atStartOfDay(zone).toInstant();
        }

        void advanceTo(LocalDate date) {
            this.currentInstant = date.atStartOfDay(zone).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}
