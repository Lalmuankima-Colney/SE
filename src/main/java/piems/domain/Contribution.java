package piems.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * General financial contribution towards a financial goal or budget (FR-12, Decision D-06, D-13).
 *
 * <p>Standard contribution record representing a dated, non-negative monetary allocation.</p>
 *
 * @param id persistent contribution identifier (or null if unpersisted)
 * @param date date of the contribution
 * @param amount positive contributed amount as {@link Money}
 */
public record Contribution(Long id, LocalDate date, Money amount) {

    /**
     * Compact constructor validating contribution invariants.
     *
     * @throws NullPointerException if date or amount is null
     * @throws IllegalArgumentException if amount is zero or negative (D-13)
     */
    public Contribution {
        Objects.requireNonNull(date, "date cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        if (amount.isZero()) {
            throw new IllegalArgumentException("Contribution amount must be strictly positive");
        }
    }

    /**
     * Constructs an unpersisted contribution.
     *
     * @param date date of the contribution
     * @param amount positive contributed amount
     */
    public Contribution(LocalDate date, Money amount) {
        this(null, date, amount);
    }

    /**
     * Converts this contribution into a specialized {@link GoalContribution}.
     *
     * @return GoalContribution instance
     */
    public GoalContribution toGoalContribution() {
        return new GoalContribution(id, date, amount);
    }
}
