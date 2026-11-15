package piems.domain;

import java.util.Objects;

/**
 * Account representing a credit card facility with a configured credit limit.
 *
 * <p>Following Decision D-31, debt and credit card liabilities are tracked explicitly
 * through non-negative {@link Money} variables. Debits beyond the configured credit
 * limit throw {@link InsufficientFundsException} (Decision Table R3, R4).</p>
 */
public class CreditCardAccount extends Account {

    private final Money creditLimit;
    private Money outstandingDebt;

    /**
     * Constructs a CreditCardAccount with credit limit and initial state.
     *
     * @param id          unique account ID
     * @param name        account name
     * @param creditLimit maximum permissible debt
     * @param archived    true if archived
     */
    public CreditCardAccount(Long id, String name, Money creditLimit, boolean archived) {
        super(id, name, Money.zero(), archived);
        this.creditLimit = Objects.requireNonNull(creditLimit, "Credit limit cannot be null");
        this.outstandingDebt = Money.zero(creditLimit.currency());
    }

    /**
     * Constructs a CreditCardAccount with credit limit, existing debt, and archival state.
     *
     * @param id              unique account ID
     * @param name            account name
     * @param creditLimit     maximum permissible debt
     * @param outstandingDebt initial outstanding debt
     * @param archived        true if archived
     */
    public CreditCardAccount(
            Long id,
            String name,
            Money creditLimit,
            Money outstandingDebt,
            boolean archived
    ) {
        super(id, name, Money.zero(), archived);
        this.creditLimit = Objects.requireNonNull(creditLimit, "Credit limit cannot be null");
        this.outstandingDebt = Objects.requireNonNull(outstandingDebt, "Outstanding debt cannot be null");
    }

    /**
     * Constructs an active CreditCardAccount with the specified credit limit.
     *
     * @param id          unique account ID
     * @param name        account name
     * @param creditLimit maximum permissible debt
     */
    public CreditCardAccount(Long id, String name, Money creditLimit) {
        this(id, name, creditLimit, false);
    }

    @Override
    public String type() {
        return "CREDIT_CARD";
    }

    /**
     * Returns the maximum allowable credit line.
     *
     * @return non-negative credit limit
     */
    public Money creditLimit() {
        return creditLimit;
    }

    /**
     * Returns the current outstanding debt owed on this card.
     *
     * @return non-negative debt
     */
    public Money outstandingDebt() {
        return outstandingDebt;
    }

    /**
     * Returns the remaining credit capacity available for charges.
     *
     * @return non-negative available credit
     */
    public Money availableCredit() {
        return creditLimit.subtract(outstandingDebt);
    }

    @Override
    public Money balance() {
        return Money.zero(creditLimit.currency());
    }

    @Override
    public boolean canDebit(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (isArchived()) {
            return false;
        }
        return outstandingDebt.minorUnits() + amount.minorUnits() <= creditLimit.minorUnits();
    }

    @Override
    public void debit(Money amount) throws InsufficientFundsException {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (isArchived()) {
            throw new InsufficientFundsException("Cannot debit archived account: " + name());
        }
        if (!canDebit(amount)) {
            throw new InsufficientFundsException(
                    "Credit limit exceeded: limit " + creditLimit + ", debt "
                            + outstandingDebt + ", requested " + amount);
        }
        this.outstandingDebt = this.outstandingDebt.add(amount);
    }

    @Override
    public void credit(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.minorUnits() >= outstandingDebt.minorUnits()) {
            this.outstandingDebt = Money.zero(creditLimit.currency());
        } else {
            this.outstandingDebt = this.outstandingDebt.subtract(amount);
        }
    }
}
