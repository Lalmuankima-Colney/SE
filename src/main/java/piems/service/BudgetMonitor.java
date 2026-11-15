package piems.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import piems.domain.Budget;
import piems.domain.BudgetStatus;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.domain.ValidationException;

/**
 * Subject in the Observer pattern managing budget alerting and hysteresis (FR-10, D-09, D-28).
 *
 * <p>Notifies registered {@link BudgetListener} observers when spending crosses the 80% (WARNING)
 * or 100% (EXCEEDED) threshold. Threshold alerts are emitted at most once per crossing.
 * Under hysteresis (Decision D-28), falling below 80% resets the state to OK, re-arming
 * the WARNING and EXCEEDED alerts. A listener that throws never interrupts transaction
 * recording (Decision D-09).</p>
 */
public class BudgetMonitor {

    private final List<BudgetListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<BudgetKey, Budget> registeredBudgets = new ConcurrentHashMap<>();
    private final Map<BudgetKey, BudgetStatus> lastNotifiedStatus = new ConcurrentHashMap<>();
    private final Clock clock;

    /**
     * Constructs a BudgetMonitor with the system default clock.
     */
    public BudgetMonitor() {
        this(Clock.systemDefaultZone());
    }

    /**
     * Constructs a BudgetMonitor with an injected clock provider (Decision D-14).
     *
     * @param clock clock provider for deterministic date operations
     */
    public BudgetMonitor(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    /**
     * Registers a budget listener observer.
     *
     * @param listener listener to register
     */
    public void registerListener(BudgetListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregisters a budget listener observer.
     *
     * @param listener listener to remove
     */
    public void unregisterListener(BudgetListener listener) {
        listeners.remove(listener);
    }

    /**
     * Alias for {@link #registerListener(BudgetListener)}.
     *
     * @param listener listener to register
     */
    public void addListener(BudgetListener listener) {
        registerListener(listener);
    }

    /**
     * Alias for {@link #unregisterListener(BudgetListener)}.
     *
     * @param listener listener to remove
     */
    public void removeListener(BudgetListener listener) {
        unregisterListener(listener);
    }

    /**
     * Returns an unmodifiable view of all registered listeners.
     *
     * @return registered budget listeners
     */
    public List<BudgetListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    /**
     * Registers a budget and enforces at most one budget per category per month (FR-09, TC-FR09-01).
     *
     * @param budget budget to register
     * @throws ValidationException if a budget is already registered for this category and month
     */
    public void registerBudget(Budget budget) throws ValidationException {
        Objects.requireNonNull(budget, "budget cannot be null");
        BudgetKey key = new BudgetKey(budget.getCategory(), budget.getMonth());
        if (registeredBudgets.containsKey(key)) {
            throw new ValidationException(
                    "A budget already exists for category '" + budget.getCategory().name()
                            + "' in " + budget.getMonth());
        }
        registeredBudgets.put(key, budget);
    }

    /**
     * Retrieves a registered budget for the given category and month.
     *
     * @param category category component
     * @param month target calendar month
     * @return Optional containing the budget, or empty if none registered
     */
    public Optional<Budget> getBudget(CategoryComponent category, YearMonth month) {
        if (category == null || month == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(registeredBudgets.get(new BudgetKey(category, month)));
    }

    /**
     * Lists all registered budgets.
     *
     * @return list of budgets
     */
    public List<Budget> listBudgets() {
        return List.copyOf(registeredBudgets.values());
    }

    /**
     * Clears all registered budgets and hysteresis tracking state.
     */
    public void clearBudgets() {
        registeredBudgets.clear();
        lastNotifiedStatus.clear();
    }

    /**
     * Evaluates total spending against a budget and dispatches alerts upon threshold crossing (FR-10, D-28).
     *
     * @param budget budget to evaluate
     * @param spent total recorded expenditure
     * @param date transaction date or evaluation date (defaults to clock if null)
     * @return evaluated BudgetStatus
     */
    public BudgetStatus check(Budget budget, Money spent, LocalDate date) {
        Objects.requireNonNull(budget, "budget cannot be null");
        Objects.requireNonNull(spent, "spent cannot be null");
        LocalDate evalDate = (date != null) ? date : LocalDate.now(clock);

        BudgetStatus currentStatus = budget.evaluate(spent);
        BudgetKey key = new BudgetKey(budget.getCategory(), budget.getMonth());
        BudgetStatus previousStatus = lastNotifiedStatus.getOrDefault(key, BudgetStatus.OK);

        if (currentStatus == BudgetStatus.OK) {
            handleOkStatus(key, previousStatus);
            return BudgetStatus.OK;
        }
        if (currentStatus == BudgetStatus.WARNING) {
            handleWarningStatus(key, previousStatus, budget, spent, evalDate);
            return BudgetStatus.WARNING;
        }
        if (currentStatus == BudgetStatus.EXCEEDED) {
            handleExceededStatus(key, previousStatus, budget, spent, evalDate);
            return BudgetStatus.EXCEEDED;
        }
        return currentStatus;
    }

    /**
     * Evaluates total spending against the registered budget for the category and date (FR-10).
     *
     * @param category category component
     * @param spent total recorded expenditure
     * @param date transaction or evaluation date
     * @return evaluated BudgetStatus, or OK if no budget registered
     */
    public BudgetStatus check(CategoryComponent category, Money spent, LocalDate date) {
        if (category == null || spent == null) {
            return BudgetStatus.OK;
        }
        LocalDate evalDate = (date != null) ? date : LocalDate.now(clock);
        YearMonth month = YearMonth.from(evalDate);
        Optional<Budget> budget = getBudget(category, month);
        return budget.map(b -> check(b, spent, evalDate)).orElse(BudgetStatus.OK);
    }

    /**
     * Returns the last notified threshold status for a category and month (Decision D-28).
     *
     * @param category category component
     * @param month target calendar month
     * @return last notified BudgetStatus (defaults to OK)
     */
    public BudgetStatus getLastNotifiedStatus(CategoryComponent category, YearMonth month) {
        if (category == null || month == null) {
            return BudgetStatus.OK;
        }
        return lastNotifiedStatus.getOrDefault(new BudgetKey(category, month), BudgetStatus.OK);
    }

    private void handleOkStatus(BudgetKey key, BudgetStatus previousStatus) {
        if (previousStatus != BudgetStatus.OK) {
            lastNotifiedStatus.put(key, BudgetStatus.OK);
        }
    }

    private void handleWarningStatus(
            BudgetKey key, BudgetStatus prev, Budget b, Money spent, LocalDate d
    ) {
        if (prev == BudgetStatus.OK) {
            lastNotifiedStatus.put(key, BudgetStatus.WARNING);
            notifyListeners(new BudgetAlertEvent(b, spent, BudgetStatus.WARNING, d));
        }
    }

    private void handleExceededStatus(
            BudgetKey key, BudgetStatus prev, Budget b, Money spent, LocalDate d
    ) {
        if (prev != BudgetStatus.EXCEEDED) {
            lastNotifiedStatus.put(key, BudgetStatus.EXCEEDED);
            notifyListeners(new BudgetAlertEvent(b, spent, BudgetStatus.EXCEEDED, d));
        }
    }

    private void notifyListeners(BudgetAlertEvent event) {
        for (BudgetListener listener : listeners) {
            try {
                listener.onBudgetAlert(event);
            } catch (Exception ignored) {
                // Decision D-09: Observer alerts never propagate exceptions to caller.
            }
        }
    }

    private record BudgetKey(CategoryComponent category, YearMonth month) {
        private BudgetKey {
            Objects.requireNonNull(category, "category cannot be null");
            Objects.requireNonNull(month, "month cannot be null");
        }
    }
}
