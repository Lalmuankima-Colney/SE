package piems.domain;

import java.util.Objects;

/**
 * Account representing a bank checking or savings deposit account.
 *
 * <p>Following Decision D-27 and Q-001, bank overdraft is prohibited in v1.0.
 * Debits exceeding current balance throw {@link InsufficientFundsException}.</p>
 */
public class BankAccount extends Account {

    private Money currentBalance;

    /**
     * Constructs a BankAccount with initial state and archival flag.
     *
     * @param id             unique account ID
     * @param name           account name
     * @param openingBalance initial opening balance
     * @param archived       true if archived
     */
    public BankAccount(Long id, String name, Money openingBalance, boolean archived) {
        super(id, name, openingBalance, archived);
        this.currentBalance = openingBalance;
    }

    /**
     * Constructs a BankAccount with explicit derived current balance and archival flag.
     *
     * @param id             unique account ID
     * @param name           account name
     * @param openingBalance initial opening balance
     * @param currentBalance derived current balance
     * @param archived       true if archived
     */
    public BankAccount(
            Long id,
            String name,
            Money openingBalance,
            Money currentBalance,
            boolean archived
    ) {
        super(id, name, openingBalance, archived);
        this.currentBalance = Objects.requireNonNull(currentBalance, "Current balance cannot be null");
    }

    /**
     * Constructs an active BankAccount.
     *
     * @param id             unique account ID
     * @param name           account name
     * @param openingBalance initial opening balance
     */
    public BankAccount(Long id, String name, Money openingBalance) {
        super(id, name, openingBalance);
        this.currentBalance = openingBalance;
    }

    @Override
    public String type() {
        return "BANK";
    }

    @Override
    public Money balance() {
        return currentBalance;
    }

    @Override
    public boolean canDebit(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (isArchived()) {
            return false;
        }
        return currentBalance.minorUnits() >= amount.minorUnits();
    }

    @Override
    public void debit(Money amount) throws InsufficientFundsException {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (isArchived()) {
            throw new InsufficientFundsException("Cannot debit archived account: " + name());
        }
        if (!canDebit(amount)) {
            throw new InsufficientFundsException(
                    "Insufficient bank funds (overdraft prohibited in v1.0): requested "
                            + amount + ", available " + currentBalance);
        }
        this.currentBalance = this.currentBalance.subtract(amount);
    }

    @Override
    public void credit(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        this.currentBalance = this.currentBalance.add(amount);
    }
}
