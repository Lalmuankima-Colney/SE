package piems.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.service.BudgetAlertEvent;
import piems.service.BudgetMonitor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box test suite for state transition tables for SavingsGoal and Budget.
 *
 * <p>Sources of truth: docs/SRS.md FR-10 and FR-12, docs/TEST_PLAN.md Section 9.2,
 * Decision Q-008, D-28, and D-35.</p>
 */
public class StateTransitionBlackBoxTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneId.of("UTC"));

    /**
     * SavingsGoal State Table Transition: ACTIVE to ACTIVE.
     * TC-FR12-05: Goal target 1000.00, contribution 400.00 -> remains ACTIVE with 40.0% progress.
     */
    @Test
    @DisplayName("TC-FR12-05: SavingsGoal remains ACTIVE while contributions are below target")
    public void goalRemainsActiveWhenContributionsBelowTarget() {
        SavingsGoal goal = new SavingsGoal(
                1L, "Emergency Fund", Money.of("1000.00"), LocalDate.of(2026, 12, 31));
        assertEquals(GoalStatus.ACTIVE, goal.status());
        assertEquals(0.0, goal.progressPercentage());

        goal.addContribution(new GoalContribution(1L, LocalDate.of(2026, 6, 1), Money.of("400.00")));
        assertEquals(GoalStatus.ACTIVE, goal.status());
        assertEquals(40.0, goal.progressPercentage());
    }

    /**
     * SavingsGoal State Table Transition: ACTIVE to ACHIEVED.
     * TC-FR12-06: Goal target 1000.00, contributions reach exactly 1000.00 -> transitions to ACHIEVED.
     */
    @Test
    @DisplayName("TC-FR12-06: SavingsGoal transitions from ACTIVE to ACHIEVED at 100% target")
    public void goalTransitionsFromActiveToAchievedWhenTargetReached() {
        SavingsGoal goal = new SavingsGoal(
                1L, "Emergency Fund", Money.of("1000.00"), LocalDate.of(2026, 12, 31));
        goal.addContribution(new GoalContribution(1L, LocalDate.of(2026, 6, 1), Money.of("600.00")));
        assertEquals(GoalStatus.ACTIVE, goal.status());

        goal.addContribution(new GoalContribution(2L, LocalDate.of(2026, 6, 15), Money.of("400.00")));
        assertEquals(GoalStatus.ACHIEVED, goal.status());
        assertEquals(100.0, goal.progressPercentage());
    }

    /**
     * SavingsGoal State Table Transition: ACHIEVED to ACHIEVED.
     * TC-FR12-07: Goal target 1000.00, contribution 1200.00 -> remains ACHIEVED with 120.0% progress.
     */
    @Test
    @DisplayName("TC-FR12-07: SavingsGoal remains ACHIEVED when contributions exceed target")
    public void goalRemainsAchievedWhenContributionsExceedTarget() {
        SavingsGoal goal = new SavingsGoal(
                1L, "Emergency Fund", Money.of("1000.00"), LocalDate.of(2026, 12, 31));
        goal.addContribution(new GoalContribution(1L, LocalDate.of(2026, 6, 1), Money.of("1200.00")));

        assertEquals(GoalStatus.ACHIEVED, goal.status());
        assertEquals(120.0, goal.progressPercentage());
    }

    /**
     * Budget State Table Transition: OK -> WARNING -> EXCEEDED.
     * TC-FR10-13: Limit 100.00; spend 79.99 (OK), 80.00 (WARNING alert), 100.00 (EXCEEDED alert).
     */
    @Test
    @DisplayName("TC-FR10-13: Budget transitions OK to WARNING to EXCEEDED upon rising expenses")
    public void budgetTransitionsFromOkToWarningToExceededUponRisingExpenses() throws Exception {
        Category category = new Category(10L, "Dining", "EXPENSE");
        Budget budget = new Budget(category, YearMonth.of(2026, 6), Money.of("100.00"));
        BudgetMonitor monitor = new BudgetMonitor(FIXED_CLOCK);
        monitor.registerBudget(budget);

        List<BudgetAlertEvent> events = new ArrayList<>();
        monitor.registerListener(events::add);

        assertEquals(BudgetStatus.OK, monitor.check(budget, Money.of("79.99"), LocalDate.of(2026, 6, 5)));
        assertTrue(events.isEmpty());

        assertEquals(BudgetStatus.WARNING, monitor.check(budget, Money.of("80.00"), LocalDate.of(2026, 6, 10)));
        assertEquals(1, events.size());
        assertEquals(BudgetStatus.WARNING, events.get(0).status());

        assertEquals(BudgetStatus.EXCEEDED, monitor.check(budget, Money.of("100.00"), LocalDate.of(2026, 6, 15)));
        assertEquals(2, events.size());
        assertEquals(BudgetStatus.EXCEEDED, events.get(1).status());
    }

    /**
     * Budget State Table Transition: EXCEEDED -> OK (reset) -> WARNING (re-alert).
     * TC-FR10-14: Limit 100.00, EXCEEDED; expense reduction to 50.00 resets to OK; re-rise to 80.00 re-alerts.
     */
    @Test
    @DisplayName("TC-FR10-14: Budget resets to OK when spending drops below 80% and re-alerts upon rising")
    public void budgetResetsToOkWhenSpendingDropsBelowEightyPercentAndReAlertsOnRising() throws Exception {
        Category category = new Category(10L, "Dining", "EXPENSE");
        Budget budget = new Budget(category, YearMonth.of(2026, 6), Money.of("100.00"));
        BudgetMonitor monitor = new BudgetMonitor(FIXED_CLOCK);
        monitor.registerBudget(budget);

        List<BudgetAlertEvent> events = new ArrayList<>();
        monitor.registerListener(events::add);

        monitor.check(budget, Money.of("105.00"), LocalDate.of(2026, 6, 10));
        assertEquals(1, events.size());
        assertEquals(BudgetStatus.EXCEEDED, events.get(0).status());

        assertEquals(BudgetStatus.OK, monitor.check(budget, Money.of("50.00"), LocalDate.of(2026, 6, 12)));
        assertEquals(1, events.size());

        assertEquals(BudgetStatus.WARNING, monitor.check(budget, Money.of("85.00"), LocalDate.of(2026, 6, 14)));
        assertEquals(2, events.size());
        assertEquals(BudgetStatus.WARNING, events.get(1).status());
    }
}
