package piems.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import piems.domain.Budget;
import piems.domain.BudgetStatus;
import piems.domain.Category;
import piems.domain.Money;
import piems.domain.ValidationException;

/**
 * Unit tests for {@link BudgetMonitor} threshold alerting, hysteresis, and observer isolation
 * (FR-10, D-09, D-28, TC-FR10-01 to TC-FR10-06).
 */
class BudgetMonitorTest {

    private BudgetMonitor monitor;
    private Category foodCategory;
    private YearMonth april2026;
    private LocalDate evalDate;
    private Budget foodBudget;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-15T10:00:00Z"), ZoneId.of("UTC"));
        monitor = new BudgetMonitor(fixedClock);
        foodCategory = new Category(1L, "Food", "EXPENSE");
        april2026 = YearMonth.of(2026, 4);
        evalDate = LocalDate.of(2026, 4, 15);
        foodBudget = new Budget(10L, foodCategory, april2026, Money.of("100.00"));
    }

    @Test
    @DisplayName("FR-10 / TC-FR10-02: Emits WARNING alert exactly once when reaching 80% threshold")
    void alertsOnceAtEightyPercent() {
        List<BudgetAlertEvent> events = new ArrayList<>();
        monitor.registerListener(events::add);

        BudgetStatus s1 = monitor.check(foodBudget, Money.of("79.99"), evalDate);
        assertThat(s1).isEqualTo(BudgetStatus.OK);
        assertThat(events).isEmpty();

        BudgetStatus s2 = monitor.check(foodBudget, Money.of("80.00"), evalDate);
        assertThat(s2).isEqualTo(BudgetStatus.WARNING);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).status()).isEqualTo(BudgetStatus.WARNING);
        assertThat(events.get(0).spent()).isEqualTo(Money.of("80.00"));

        BudgetStatus s3 = monitor.check(foodBudget, Money.of("95.00"), evalDate);
        assertThat(s3).isEqualTo(BudgetStatus.WARNING);
        assertThat(events).hasSize(1);
    }

    @Test
    @DisplayName("FR-10 / TC-FR10-04: Emits EXCEEDED alert exactly once when reaching 100% threshold")
    void alertsOnceAtOneHundredPercent() {
        List<BudgetAlertEvent> events = new ArrayList<>();
        monitor.registerListener(events::add);

        monitor.check(foodBudget, Money.of("80.00"), evalDate);
        assertThat(events).hasSize(1);

        BudgetStatus s100 = monitor.check(foodBudget, Money.of("100.00"), evalDate);
        assertThat(s100).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(events).hasSize(2);
        assertThat(events.get(1).status()).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(events.get(1).spent()).isEqualTo(Money.of("100.00"));

        BudgetStatus s120 = monitor.check(foodBudget, Money.of("120.00"), evalDate);
        assertThat(s120).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(events).hasSize(2);
    }

    @Test
    @DisplayName("D-09: A throwing listener never propagates exception or breaks monitoring")
    void throwingListenerDoesNotBreakRecording() {
        List<BudgetAlertEvent> healthyEvents = new ArrayList<>();
        monitor.registerListener(event -> {
            throw new RuntimeException("Simulated observer crash");
        });
        monitor.registerListener(healthyEvents::add);

        assertThatCode(() -> monitor.check(foodBudget, Money.of("80.00"), evalDate))
                .doesNotThrowAnyException();
        assertThat(healthyEvents).hasSize(1);
        assertThat(healthyEvents.get(0).status()).isEqualTo(BudgetStatus.WARNING);
    }

    @Test
    @DisplayName("TC-FR10-06 / D-28: Spending dropping below 80% resets state to OK, re-arming WARNING alert")
    void resetToOkWhenDroppingBelowEightyPercent() {
        List<BudgetAlertEvent> events = new ArrayList<>();
        monitor.registerListener(events::add);

        monitor.check(foodBudget, Money.of("80.00"), evalDate);
        assertThat(events).hasSize(1);
        assertThat(monitor.getLastNotifiedStatus(foodCategory, april2026)).isEqualTo(BudgetStatus.WARNING);

        BudgetStatus resetStatus = monitor.check(foodBudget, Money.of("75.00"), evalDate);
        assertThat(resetStatus).isEqualTo(BudgetStatus.OK);
        assertThat(monitor.getLastNotifiedStatus(foodCategory, april2026)).isEqualTo(BudgetStatus.OK);
        assertThat(events).hasSize(1);

        monitor.check(foodBudget, Money.of("80.00"), evalDate);
        assertThat(events).hasSize(2);
        assertThat(events.get(1).status()).isEqualTo(BudgetStatus.WARNING);
    }

    @Test
    @DisplayName("Observer subscription management: unregistering stops notifications")
    void unregisterListenerStopsReceivingAlerts() {
        List<BudgetAlertEvent> events = new ArrayList<>();
        BudgetListener listener = events::add;
        monitor.addListener(listener);
        assertThat(monitor.getListeners()).contains(listener);

        monitor.removeListener(listener);
        assertThat(monitor.getListeners()).doesNotContain(listener);

        monitor.check(foodBudget, Money.of("80.00"), evalDate);
        assertThat(events).isEmpty();
    }

    @Test
    @DisplayName("TC-FR09-01: Registering duplicate budget for same category and month throws ValidationException")
    void duplicateBudgetRegistrationThrowsValidationException() throws ValidationException {
        monitor.registerBudget(foodBudget);
        assertThat(monitor.listBudgets()).contains(foodBudget);

        Budget duplicate = new Budget(11L, foodCategory, april2026, Money.of("200.00"));
        assertThatThrownBy(() -> monitor.registerBudget(duplicate))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists for category 'Food'");

        monitor.clearBudgets();
        assertThat(monitor.listBudgets()).isEmpty();
    }

    @Test
    @DisplayName("check by CategoryComponent delegates to registered budget or returns OK if absent")
    void checkByCategoryComponentDelegation() throws ValidationException {
        List<BudgetAlertEvent> events = new ArrayList<>();
        monitor.registerListener(events::add);

        BudgetStatus statusNoBudget = monitor.check(foodCategory, Money.of("100.00"), evalDate);
        assertThat(statusNoBudget).isEqualTo(BudgetStatus.OK);
        assertThat(events).isEmpty();

        monitor.registerBudget(foodBudget);
        BudgetStatus statusWithBudget = monitor.check(foodCategory, Money.of("85.00"), evalDate);
        assertThat(statusWithBudget).isEqualTo(BudgetStatus.WARNING);
        assertThat(events).hasSize(1);
    }

    @Test
    @DisplayName("Defensive null checks on BudgetMonitor methods")
    void nullArgumentsValidation() {
        assertThatThrownBy(() -> monitor.registerListener(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> monitor.registerBudget(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> monitor.check((Budget) null, Money.of("10.00"), evalDate))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> monitor.check(foodBudget, null, evalDate))
                .isInstanceOf(NullPointerException.class);
        assertThat(monitor.check((Category) null, Money.of("10.00"), evalDate))
                .isEqualTo(BudgetStatus.OK);
        assertThat(monitor.getLastNotifiedStatus(null, april2026))
                .isEqualTo(BudgetStatus.OK);
    }

    @Test
    @DisplayName("TC-FR10-10 Registering duplicate listener instance is idempotent")
    void registerListenerIgnoresDuplicateRegistration() {
        BudgetListener listener = event -> {};
        monitor.registerListener(listener);
        int initialCount = monitor.getListeners().size();

        monitor.registerListener(listener);
        assertThat(monitor.getListeners()).hasSize(initialCount);
    }

    @Test
    @DisplayName("TC-FR10-11 Spending more while already in EXCEEDED status does not emit duplicate alerts")
    void subsequentExpenseWhenAlreadyExceededDoesNotRealert() {
        List<BudgetAlertEvent> events = new ArrayList<>();
        monitor.registerListener(events::add);

        // Reach EXCEEDED
        monitor.check(foodBudget, Money.of("100.00"), evalDate);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).status()).isEqualTo(BudgetStatus.EXCEEDED);

        // Spend even more while already in EXCEEDED
        monitor.check(foodBudget, Money.of("120.00"), evalDate);
        assertThat(events).hasSize(1);
    }
}
