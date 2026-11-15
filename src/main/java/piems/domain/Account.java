package piems.domain;

import java.util.Objects;

/**
 * Abstract base class for financial accounts in PIEMS.
 *
 * <p>Enforces identity, naming, opening balance retention, and soft-delete archiving
 * according to Decision D-07, D-26, and Requirement FR-01.</p>
 */
public abstract class Account {

    private final Long id;
    private final String name;
    private final Money openingBalance;
    private boolean archived;

    /**
     * Constructs an Account with initial state and archival status.
     *
     * @param id             unique account identifier, or null if unpersisted
     * @param name           account display name
     * @param openingBalance initial opening balance
     * @param archived       true if the account is archived
     */
    public Account(Long id, String name, Money openingBalance, boolean archived) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be null or empty");
        }
        this.id = id;
        this.name = name.trim();
        this.openingBalance = Objects.requireNonNull(openingBalance, "Opening balance cannot be null");
        this.archived = archived;
    }

    /**
     * Constructs an active Account with the specified opening balance.
     *
     * @param id             unique account identifier, or null if unpersisted
     * @param name           account display name
     * @param openingBalance initial opening balance
     */
    public Account(Long id, String name, Money openingBalance) {
        this(id, name, openingBalance, false);
    }

    /**
     * Returns the account identifier.
     *
     * @return account ID, or null if not yet persisted
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the account display name.
     *
     * @return non-empty name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the initial opening balance.
     *
     * @return non-negative opening Money
     */
    public Money openingBalance() {
        return openingBalance;
    }

    /**
     * Returns whether this account has been archived.
     *
     * @return true if archived, rejecting new debit transactions
     */
    public boolean isArchived() {
        return archived;
    }

    /**
     * Archives this account, preventing any subsequent debits.
     */
    public void archive() {
        this.archived = true;
    }

    /**
     * Returns the dynamically derived current balance of this account.
     *
     * @return current balance
     */
    public abstract Money balance();

    /**
     * Evaluates whether a debit of the specified amount can be accepted.
     *
     * @param amount monetary debit amount to evaluate
     * @return true if the debit is valid and acceptable
     */
    public abstract boolean canDebit(Money amount);

    /**
     * Debits the specified amount from this account.
     *
     * @param amount monetary debit amount
     * @throws InsufficientFundsException if the debit exceeds funds/limit or account is archived
     */
    public abstract void debit(Money amount) throws InsufficientFundsException;

    /**
     * Credits the specified amount to this account.
     *
     * @param amount monetary credit amount
     */
    public abstract void credit(Money amount);

    /**
     * Applies the effect of the specified transaction to this account.
     *
     * @param tx transaction to apply
     * @throws InsufficientFundsException if the account cannot accept the transaction debit
     */
    public void apply(Transaction tx) throws InsufficientFundsException {
        Objects.requireNonNull(tx, "Transaction cannot be null");
        tx.applyTo(this);
    }

    /**
     * Reverses the effect of the specified transaction on this account.
     *
     * @param tx transaction to reverse
     * @throws InsufficientFundsException if the account cannot accept the reversal debit
     */
    public void reverse(Transaction tx) throws InsufficientFundsException {
        Objects.requireNonNull(tx, "Transaction cannot be null");
        tx.reverseOn(this);
    }

    /**
     * Returns the account type string identifier ("CASH", "BANK", "CREDIT_CARD").
     *
     * @return account type string
     */
    public abstract String type();

    /**
     * Returns the maximum allowable credit line for credit card accounts, or zero for cash/bank.
     *
     * @return non-negative credit limit Money
     */
    public Money creditLimit() {
        return Money.zero(openingBalance.currency());
    }
}
