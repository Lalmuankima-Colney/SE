package piems.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Immutable financial contribution towards a savings goal (FR-12, Decision D-06, D-13).
 *
 * @param id persistent contribution identifier (or null if unpersisted)
 * @param date date of the contribution
 * @param amount positive contributed amount as {@link Money}
 */
public record GoalContribution(Long id, LocalDate date, Money amount) {

    /**
     * Compact constructor validating contribution invariants.
     *
     * @throws NullPointerException if date or amount is null
     * @throws IllegalArgumentException if amount is zero or negative (D-13)
     */
    public GoalContribution {
        Objects.requireNonNull(date, "date cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        if (amount.isZero()) {
            throw new IllegalArgumentException("Contribution amount must be strictly positive");
        }
    }

    /**
     * Constructs an unpersisted goal contribution.
     *
     * @param date date of the contribution
     * @param amount positive contributed amount
     */
    public GoalContribution(LocalDate date, Money amount) {
        this(null, date, amount);
    }
}
