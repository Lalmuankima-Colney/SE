package piems.domain;

/**
 * Status of spending against a budget limit (FR-10).
 */
public enum BudgetStatus {
    /** Spending is below 80% of the budget limit. */
    OK,

    /** Spending is at least 80% but below 100% of the budget limit. */
    WARNING,

    /** Spending is at or above 100% of the budget limit. */
    EXCEEDED
}
