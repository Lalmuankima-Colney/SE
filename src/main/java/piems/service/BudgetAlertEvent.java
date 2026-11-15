package piems.service;

import java.time.LocalDate;
import java.util.Objects;
import piems.domain.Budget;
import piems.domain.BudgetStatus;
import piems.domain.Money;

/**
 * Event object emitted when a budget threshold is crossed (Decision D-09, FR-10).
 *
 * <p>Captures the triggering budget, accumulated spending, resulting threshold status,
 * and event date without throwing exceptions (Decision D-09).</p>
 *
 * @param budget the budget whose threshold was reached
 * @param spent total accumulated expenditure for the budget period
 * @param status the threshold crossed (WARNING or EXCEEDED)
 * @param date date of the transaction or evaluation triggering the alert
 */
public record BudgetAlertEvent(
        Budget budget,
        Money spent,
        BudgetStatus status,
        LocalDate date
) {

    /**
     * Compact constructor validating event invariants.
     *
     * @throws NullPointerException if any parameter is null
     */
    public BudgetAlertEvent {
        Objects.requireNonNull(budget, "budget cannot be null");
        Objects.requireNonNull(spent, "spent cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(date, "date cannot be null");
    }
}
