package piems.domain;

import java.time.YearMonth;
import java.util.Objects;

/**
 * Budget defined for a specific category and calendar month (FR-09, FR-10).
 *
 * <p>Enforces positive limits and evaluates spending thresholds using integer minor-unit
 * arithmetic without floating-point rounding errors (Decision D-06, D-09, D-13, Q-011).</p>
 */
public final class Budget {

    private final Long id;
    private final CategoryComponent category;
    private final YearMonth month;
    private final Money limit;

    /**
     * Constructs a budget with an existing persistent identifier.
     *
     * @param id persistent budget ID, or null if unpersisted
     * @param category category or category group for this budget
     * @param month target calendar year and month
     * @param limit maximum spending limit (must be strictly positive)
     * @throws IllegalArgumentException if limit is zero or non-positive (Q-011, D-13)
     * @throws NullPointerException if any mandatory parameter is null
     */
    public Budget(Long id, CategoryComponent category, YearMonth month, Money limit) {
        this.id = id;
        this.category = Objects.requireNonNull(category, "category cannot be null");
        this.month = Objects.requireNonNull(month, "month cannot be null");
        this.limit = Objects.requireNonNull(limit, "limit cannot be null");
        if (limit.isZero()) {
            throw new IllegalArgumentException("Budget limit must be strictly positive");
        }
    }

    /**
     * Constructs an unpersisted budget.
     *
     * @param category category or category group for this budget
     * @param month target calendar year and month
     * @param limit maximum spending limit (must be strictly positive)
     */
    public Budget(CategoryComponent category, YearMonth month, Money limit) {
        this(null, category, month, limit);
    }

    /**
     * Evaluates total spending against this budget's limit.
     *
     * <p>Cyclomatic complexity V(G) = 4 with four independent paths:
     * 1. Limit is zero/non-positive (throws IllegalArgumentException, Q-011)
     * 2. Ratio &gt;= 1.0 (returns EXCEEDED, FR-10)
     * 3. Ratio &gt;= 0.8 (returns WARNING, FR-10)
     * 4. Ratio &lt; 0.8 (returns OK, FR-10)</p>
     *
     * @param spent total recorded spending for this budget category and month
     * @return resulting {@link BudgetStatus}
     * @throws IllegalArgumentException if limit is zero or non-positive (Q-011)
     * @throws NullPointerException if spent is null
     */
    public BudgetStatus evaluate(Money spent) {
        Objects.requireNonNull(spent, "spent cannot be null");
        long spentMinor = spent.minorUnits();
        long limitMinor = limit.minorUnits();
        if (spentMinor * 100L >= limitMinor * 100L) {
            return BudgetStatus.EXCEEDED;
        }
        if (spentMinor * 100L >= limitMinor * 80L) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.OK;
    }

    /**
     * Checks if this budget matches the given category and month (FR-09).
     *
     * @param otherCategory category to match
     * @param otherMonth month to match
     * @return true if both category and month match, false otherwise
     */
    public boolean matches(CategoryComponent otherCategory, YearMonth otherMonth) {
        if (otherCategory == null || otherMonth == null) {
            return false;
        }
        return this.category.equals(otherCategory) && this.month.equals(otherMonth);
    }

    /**
     * Returns the persistent identifier.
     *
     * @return budget ID, or null if unpersisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the budgeted category component.
     *
     * @return category component
     */
    public CategoryComponent getCategory() {
        return category;
    }

    /**
     * Returns the budgeted calendar year and month.
     *
     * @return budgeted YearMonth
     */
    public YearMonth getMonth() {
        return month;
    }

    /**
     * Returns the budget limit amount.
     *
     * @return limit as {@link Money}
     */
    public Money getLimit() {
        return limit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Budget budget)) {
            return false;
        }
        return Objects.equals(id, budget.id)
                && category.equals(budget.category)
                && month.equals(budget.month)
                && limit.equals(budget.limit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, category, month, limit);
    }

    @Override
    public String toString() {
        return "Budget{"
                + "id=" + id
                + ", category=" + category.name()
                + ", month=" + month
                + ", limit=" + limit
                + '}';
    }
}
