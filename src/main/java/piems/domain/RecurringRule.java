package piems.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Recurring transaction rule entity (Requirement FR-11).
 *
 * <p>Encapsulates transaction template data, recurrence schedule, and tracks
 * the latest generation timestamp to support catch-up and prevent duplicate entries.</p>
 */
public final class RecurringRule {

    private final Long id;
    private final String description;
    private final Money amount;
    private final Account account;
    private final CategoryComponent category;
    private final String transactionType;
    private final Schedule schedule;
    private LocalDate lastGenerated;

    /**
     * Constructs a RecurringRule with all attributes.
     *
     * @param id              unique persistent identifier, or null if unpersisted
     * @param description     description or memo for generated transactions
     * @param amount          monetary transaction amount (must be positive)
     * @param account         associated account
     * @param category        associated category component, or null
     * @param transactionType transaction kind ("EXPENSE" or "INCOME")
     * @param schedule        recurrence schedule definition
     * @param lastGenerated   last date for which a transaction was generated, or null
     * @throws IllegalArgumentException if description is blank or amount is zero
     * @throws NullPointerException     if description, amount, account, transactionType, or schedule is null
     */
    public RecurringRule(Long id, String description, Money amount, Account account,
                         CategoryComponent category, String transactionType,
                         Schedule schedule, LocalDate lastGenerated) {
        this.id = id;
        this.description = Objects.requireNonNull(description, "description cannot be null").trim();
        if (this.description.isEmpty()) {
            throw new IllegalArgumentException("description cannot be blank");
        }
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        if (amount.isZero()) {
            throw new IllegalArgumentException("Recurring transaction amount must be positive");
        }
        this.account = Objects.requireNonNull(account, "account cannot be null");
        this.category = category;
        String type = Objects.requireNonNull(transactionType, "transactionType cannot be null").trim().toUpperCase();
        if (!type.equals("EXPENSE") && !type.equals("INCOME")) {
            throw new IllegalArgumentException("transactionType must be EXPENSE or INCOME: " + transactionType);
        }
        this.transactionType = type;
        this.schedule = Objects.requireNonNull(schedule, "schedule cannot be null");
        this.lastGenerated = lastGenerated;
    }

    /**
     * Constructs an unpersisted RecurringRule without prior generations.
     *
     * @param description     description or memo
     * @param amount          monetary transaction amount
     * @param account         associated account
     * @param category        associated category component, or null
     * @param transactionType transaction kind ("EXPENSE" or "INCOME")
     * @param schedule        recurrence schedule
     */
    public RecurringRule(String description, Money amount, Account account,
                         CategoryComponent category, String transactionType,
                         Schedule schedule) {
        this(null, description, amount, account, category, transactionType, schedule, null);
    }

    /**
     * Computes all pending due dates up to {@code until} that have not yet been generated.
     *
     * @param until upper date boundary (inclusive)
     * @return unmodifiable list of due dates
     */
    public List<LocalDate> dueDates(LocalDate until) {
        return schedule.computeDueDates(lastGenerated, until);
    }

    /**
     * Updates the last generated date milestone.
     *
     * @param date latest generated transaction date
     */
    public void markGenerated(LocalDate date) {
        this.lastGenerated = Objects.requireNonNull(date, "date cannot be null");
    }

    /**
     * Returns the persistent identifier.
     *
     * @return ID, or null if unpersisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the rule description.
     *
     * @return description string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the monetary amount.
     *
     * @return amount as {@link Money}
     */
    public Money getAmount() {
        return amount;
    }

    /**
     * Returns the associated account.
     *
     * @return account instance
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Returns the category component.
     *
     * @return category component, or null
     */
    public CategoryComponent getCategory() {
        return category;
    }

    /**
     * Returns the transaction kind ("EXPENSE" or "INCOME").
     *
     * @return uppercase kind string
     */
    public String getTransactionType() {
        return transactionType;
    }

    /**
     * Returns the recurrence schedule.
     *
     * @return schedule instance
     */
    public Schedule getSchedule() {
        return schedule;
    }

    /**
     * Returns the latest date for which transactions were generated.
     *
     * @return last generated date, or null
     */
    public LocalDate getLastGenerated() {
        return lastGenerated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecurringRule that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && description.equals(that.description)
                && amount.equals(that.amount)
                && account.equals(that.account)
                && Objects.equals(category, that.category)
                && transactionType.equals(that.transactionType)
                && schedule.equals(that.schedule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, amount, account, category, transactionType, schedule);
    }

    @Override
    public String toString() {
        return "RecurringRule{"
                + "id=" + id
                + ", description='" + description + '\''
                + ", amount=" + amount
                + ", account=" + account.name()
                + ", transactionType='" + transactionType + '\''
                + ", lastGenerated=" + lastGenerated
                + '}';
    }
}
