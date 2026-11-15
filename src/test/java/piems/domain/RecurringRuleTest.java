package piems.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RecurringRule} and {@link Schedule} (FR-11, TC-FR11-03, D-29).
 */
class RecurringRuleTest {

    private Account cashAccount;
    private Category rentCategory;

    @BeforeEach
    void setUp() {
        cashAccount = new CashAccount(1L, "Wallet", Money.of("5000.00"));
        rentCategory = new Category(10L, "Rent", "EXPENSE");
    }

    @Test
    @DisplayName("TC-FR11-03 Month-end clamping: Monthly rule starting on the 31st clamps in shorter months")
    void monthlyRuleOnThe31stClampsToMonthEnd() {
        Schedule schedule = new Schedule(Schedule.Frequency.MONTHLY, LocalDate.of(2026, 1, 31));
        RecurringRule rule = new RecurringRule(
                "Apartment Rent", Money.of("1000.00"), cashAccount, rentCategory, "EXPENSE", schedule
        );

        List<LocalDate> dates = rule.dueDates(LocalDate.of(2026, 5, 1));
        assertThat(dates).containsExactly(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28), // Clamped in February (non-leap year)
                LocalDate.of(2026, 3, 31), // Full 31 days in March
                LocalDate.of(2026, 4, 30)  // Clamped in April
        );
    }

    @Test
    @DisplayName("TC-FR11-03 Daily schedule generates every consecutive calendar day")
    void dailyRuleComputesAllDates() {
        Schedule schedule = new Schedule(Schedule.Frequency.DAILY, LocalDate.of(2026, 3, 1));
        RecurringRule rule = new RecurringRule(
                "Daily Transit", Money.of("5.00"), cashAccount, null, "EXPENSE", schedule
        );

        List<LocalDate> dates = rule.dueDates(LocalDate.of(2026, 3, 5));
        assertThat(dates).containsExactly(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 3),
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 5)
        );
    }

    @Test
    @DisplayName("TC-FR11-03 Weekly schedule generates exactly every seven days")
    void weeklyRuleComputesAllDates() {
        Schedule schedule = new Schedule(Schedule.Frequency.WEEKLY, LocalDate.of(2026, 3, 1));
        RecurringRule rule = new RecurringRule(
                "Weekly Allowance", Money.of("50.00"), cashAccount, null, "INCOME", schedule
        );

        List<LocalDate> dates = rule.dueDates(LocalDate.of(2026, 3, 22));
        assertThat(dates).containsExactly(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 8),
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 22)
        );
    }

    @Test
    @DisplayName("TC-FR11-03 Schedule bounded by endDate does not generate past termination date")
    void ruleBoundedByEndDate() {
        Schedule schedule = new Schedule(
                Schedule.Frequency.MONTHLY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1)
        );
        RecurringRule rule = new RecurringRule(
                "Gym Subscription", Money.of("30.00"), cashAccount, null, "EXPENSE", schedule
        );

        List<LocalDate> dates = rule.dueDates(LocalDate.of(2026, 6, 1));
        assertThat(dates).containsExactly(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 1)
        );
    }

    @Test
    @DisplayName("TC-FR11-03 Validation: Invalid schedule or rule arguments throw IllegalArgumentException")
    void validationRejectsInvalidInputs() {
        assertThatThrownBy(() -> new Schedule(
                Schedule.Frequency.DAILY, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1)))
                .isInstanceOf(IllegalArgumentException.class);

        Schedule validSchedule = new Schedule(Schedule.Frequency.DAILY, LocalDate.of(2026, 1, 1));
        assertThatThrownBy(() -> new RecurringRule(
                "", Money.of("10.00"), cashAccount, null, "EXPENSE", validSchedule))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RecurringRule(
                "Test", Money.zero(), cashAccount, null, "EXPENSE", validSchedule))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RecurringRule(
                "Test", Money.of("10.00"), cashAccount, null, "INVALID", validSchedule))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("TC-FR11-03 Marking generated date advances future due date queries")
    void markGeneratedAdvancesDueDates() {
        Schedule schedule = new Schedule(Schedule.Frequency.MONTHLY, LocalDate.of(2026, 1, 1));
        RecurringRule rule = new RecurringRule(
                "Internet", Money.of("60.00"), cashAccount, null, "EXPENSE", schedule
        );

        List<LocalDate> firstBatch = rule.dueDates(LocalDate.of(2026, 2, 1));
        assertThat(firstBatch).containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));

        rule.markGenerated(LocalDate.of(2026, 2, 1));
        List<LocalDate> secondBatch = rule.dueDates(LocalDate.of(2026, 4, 1));
        assertThat(secondBatch).containsExactly(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1));
    }

    @Test
    @DisplayName("TC-FR11-03 Getters and toString contract for RecurringRule and Schedule")
    void gettersAndToStringContract() {
        Schedule schedule = new Schedule(
                Schedule.Frequency.MONTHLY, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 12, 31)
        );
        RecurringRule rule = new RecurringRule(
                100L, "Rent", Money.of("1000.00"), cashAccount, rentCategory, "EXPENSE", schedule, null
        );

        assertThat(rule.getId()).isEqualTo(100L);
        assertThat(rule.getDescription()).isEqualTo("Rent");
        assertThat(rule.getAmount()).isEqualTo(Money.of("1000.00"));
        assertThat(rule.getAccount()).isEqualTo(cashAccount);
        assertThat(rule.getCategory()).isEqualTo(rentCategory);
        assertThat(rule.getTransactionType()).isEqualTo("EXPENSE");
        assertThat(rule.getSchedule()).isEqualTo(schedule);
        assertThat(rule.getLastGenerated()).isNull();
        assertThat(rule.toString()).contains("Rent", "Wallet", "EXPENSE");

        assertThat(schedule.getFrequency()).isEqualTo(Schedule.Frequency.MONTHLY);
        assertThat(schedule.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(schedule.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(schedule.getTargetDayOfMonth()).isEqualTo(15);
    }

    @Test
    @DisplayName("TC-FR11-03 Equals and hashCode contract for RecurringRule")
    void equalsAndHashCodeContract() {
        Schedule schedule = new Schedule(Schedule.Frequency.MONTHLY, LocalDate.of(2026, 1, 1));
        RecurringRule rule1 = new RecurringRule(
                1L, "Sal", Money.of("2000.00"), cashAccount, null, "INCOME", schedule, null
        );
        RecurringRule rule2 = new RecurringRule(
                1L, "Sal", Money.of("2000.00"), cashAccount, null, "INCOME", schedule, null
        );
        RecurringRule diff = new RecurringRule(
                2L, "Sal", Money.of("2000.00"), cashAccount, null, "INCOME", schedule, null
        );

        assertThat(rule1).isEqualTo(rule2);
        assertThat(rule1.hashCode()).isEqualTo(rule2.hashCode());
        assertThat(rule1).isNotEqualTo(diff);
        assertThat(rule1).isNotEqualTo(null);
        assertThat(rule1).isNotEqualTo("otherString");
    }

    @Test
    @DisplayName("TC-FR11-03 Schedule boundary edge cases: until before start or fromAfter after until")
    void scheduleBoundaryEdgeCases() {
        Schedule schedule = new Schedule(Schedule.Frequency.DAILY, LocalDate.of(2026, 6, 1));
        assertThat(schedule.computeDueDates(null, LocalDate.of(2026, 5, 30))).isEmpty();
        assertThat(schedule.computeDueDates(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 5))).isEmpty();
    }

    @Test
    @DisplayName("TC-FR11-04 Unpersisted rules with identical attributes are equal")
    void rulesWithIdenticalAttributesAreEqual() {
        Schedule schedule = new Schedule(Schedule.Frequency.MONTHLY, LocalDate.of(2026, 1, 1));
        RecurringRule r1 = new RecurringRule(
                null, "Salary", Money.of("1000.00"), cashAccount, rentCategory, "INCOME", schedule, null);
        RecurringRule r2 = new RecurringRule(
                null, "Salary", Money.of("1000.00"), cashAccount, rentCategory, "INCOME", schedule, null);
        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    @DisplayName("TC-FR11-05 Unpersisted rules differing in any attribute are not equal")
    void rulesDifferingInAnyAttributeAreNotEqual() {
        Schedule s1 = new Schedule(Schedule.Frequency.MONTHLY, LocalDate.of(2026, 1, 1));
        Schedule s2 = new Schedule(Schedule.Frequency.WEEKLY, LocalDate.of(2026, 1, 1));
        Account otherAcc = new CashAccount(2L, "OtherWallet", Money.of("50.00"));
        Category otherCat = new Category(2L, "Food", "EXPENSE");

        RecurringRule base = new RecurringRule(
                null, "Base", Money.of("100.00"), cashAccount, rentCategory, "EXPENSE", s1, null);

        assertThat(base).isNotEqualTo(new RecurringRule(
                null, "Diff", Money.of("100.00"), cashAccount, rentCategory, "EXPENSE", s1, null));
        assertThat(base).isNotEqualTo(new RecurringRule(
                null, "Base", Money.of("105.00"), cashAccount, rentCategory, "EXPENSE", s1, null));
        assertThat(base).isNotEqualTo(new RecurringRule(
                null, "Base", Money.of("100.00"), otherAcc, rentCategory, "EXPENSE", s1, null));
        assertThat(base).isNotEqualTo(new RecurringRule(
                null, "Base", Money.of("100.00"), cashAccount, otherCat, "EXPENSE", s1, null));
        assertThat(base).isNotEqualTo(new RecurringRule(
                null, "Base", Money.of("100.00"), cashAccount, rentCategory, "INCOME", s1, null));
        assertThat(base).isNotEqualTo(new RecurringRule(
                null, "Base", Money.of("100.00"), cashAccount, rentCategory, "EXPENSE", s2, null));
    }

    @Test
    @DisplayName("TC-FR11-06 Schedule clamps monthly due date to month-end for shorter months")
    void scheduleClampsMonthlyDueDateToMonthEnd() {
        Schedule schedule = new Schedule(Schedule.Frequency.MONTHLY, LocalDate.of(2026, 1, 31));
        List<LocalDate> dueDates = schedule.computeDueDates(null, LocalDate.of(2026, 4, 30));
        assertThat(dueDates).containsExactly(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30)
        );
    }

    @Test
    @DisplayName("TC-FR11-07 Schedule iterates properly when fromAfter is null")
    void scheduleIteratesWhenFromAfterIsNull() {
        Schedule schedule = new Schedule(Schedule.Frequency.WEEKLY, LocalDate.of(2026, 1, 1));
        List<LocalDate> dueDates = schedule.computeDueDates(null, LocalDate.of(2026, 1, 15));
        assertThat(dueDates).containsExactly(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 8),
                LocalDate.of(2026, 1, 15)
        );
    }
}
