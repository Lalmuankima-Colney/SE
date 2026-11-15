package piems.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Abstract base class for financial transactions in PIEMS.
 *
 * <p>Enforces positive monetary amounts, account association, date retention,
 * and polymorphic effect calculation via {@link #effectOn(Account)} according
 * to Decision D-07, D-25, and Requirements FR-02, FR-03, FR-04.</p>
 */
public abstract class Transaction {

    /** Maximum allowed transaction ceiling in minor units (999,999,999.99). */
    public static final long MAX_AMOUNT_MINOR = 99_999_999_999L;

    private final Long id;
    private final LocalDate date;
    private final Money amount;
    private final String note;
    private final Account account;
    private final Long categoryId;
    private final CategoryComponent category;

    /**
     * Constructs a Transaction with CategoryComponent reference.
     *
     * @param id       unique transaction ID, or null if unpersisted
     * @param date     transaction date
     * @param amount   positive monetary amount
     * @param note     optional note or description
     * @param account  source or primary account
     * @param category associated category component
     */
    public Transaction(
            Long id, LocalDate date, Money amount, String note, Account account, CategoryComponent category) {
        this.id = id;
        this.date = Objects.requireNonNull(date, "Transaction date cannot be null");
        this.amount = Objects.requireNonNull(amount, "Transaction amount cannot be null");
        if (amount.minorUnits() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be strictly positive: " + amount);
        }
        if (amount.minorUnits() > MAX_AMOUNT_MINOR) {
            throw new IllegalArgumentException("Transaction amount exceeds ceiling of 999,999,999.99: " + amount);
        }
        this.note = note != null ? note.trim() : null;
        this.account = Objects.requireNonNull(account, "Account cannot be null");
        this.category = category;
        this.categoryId = category != null ? category.id() : null;
    }

    /**
     * Constructs a Transaction with full attributes.
     *
     * @param id         unique transaction ID, or null if unpersisted
     * @param date       transaction date
     * @param amount     positive monetary amount
     * @param note       optional note or description
     * @param account    source or primary account
     * @param categoryId optional associated category ID
     */
    public Transaction(Long id, LocalDate date, Money amount, String note, Account account, Long categoryId) {
        this.id = id;
        this.date = Objects.requireNonNull(date, "Transaction date cannot be null");
        this.amount = Objects.requireNonNull(amount, "Transaction amount cannot be null");
        if (amount.minorUnits() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be strictly positive: " + amount);
        }
        if (amount.minorUnits() > MAX_AMOUNT_MINOR) {
            throw new IllegalArgumentException("Transaction amount exceeds ceiling of 999,999,999.99: " + amount);
        }
        this.note = note != null ? note.trim() : null;
        this.account = Objects.requireNonNull(account, "Account cannot be null");
        this.category = null;
        this.categoryId = categoryId;
    }

    /**
     * Returns the transaction identifier.
     *
     * @return transaction ID, or null if unpersisted
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the transaction date.
     *
     * @return non-null date
     */
    public LocalDate date() {
        return date;
    }

    /**
     * Returns the monetary amount.
     *
     * @return positive Money amount
     */
    public Money amount() {
        return amount;
    }

    /**
     * Returns the transaction note.
     *
     * @return note, or null if none
     */
    public String note() {
        return note;
    }

    /**
     * Returns the primary or source account.
     *
     * @return non-null Account
     */
    public Account account() {
        return account;
    }

    /**
     * Returns the associated category ID.
     *
     * @return category ID, or null
     */
    public Long categoryId() {
        return categoryId;
    }

    /**
     * Returns the associated category component, if any.
     *
     * @return CategoryComponent, or null
     */
    public CategoryComponent category() {
        return category;
    }

    /**
     * Returns the uppercase kind identifier ("INCOME", "EXPENSE", "TRANSFER").
     *
     * @return transaction kind string
     */
    public abstract String kind();

    /**
     * Calculates the signed net balance effect on the specified account in minor units.
     *
     * @param target target account to evaluate
     * @return positive minor units for credit, negative for debit, or 0 if unaffected
     */
    public abstract long effectOn(Account target);

    /**
     * Applies this transaction's financial effect to the target account.
     *
     * @param target account to apply to
     * @throws InsufficientFundsException if the account cannot accept the debit
     */
    public abstract void applyTo(Account target) throws InsufficientFundsException;

    /**
     * Reverses this transaction's financial effect on the target account.
     *
     * @param target account to reverse on
     * @throws InsufficientFundsException if the account cannot accept the reversal debit
     */
    public abstract void reverseOn(Account target) throws InsufficientFundsException;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Transaction that)) {
            return false;
        }
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(date, that.date)
                && Objects.equals(amount, that.amount)
                && Objects.equals(account, that.account)
                && Objects.equals(note, that.note)
                && Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(date, amount, note, account, categoryId);
    }
}
