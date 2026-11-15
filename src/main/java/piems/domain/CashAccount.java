package piems.domain;

import java.util.Objects;

/**
 * Account representing physical cash on hand.
 *
 * <p>Enforces zero overdraft (Decision Table R1, R2) and rejects debits when archived (R5).</p>
 */
public class CashAccount extends Account {

    private Money currentBalance;

    /**
     * Constructs a CashAccount with initial state and archival flag.
     *
     * @param id             unique account ID
     * @param name           account name
     * @param openingBalance initial opening balance
     * @param archived       true if archived
     */
    public CashAccount(Long id, String name, Money openingBalance, boolean archived) {
        super(id, name, openingBalance, archived);
        this.currentBalance = openingBalance;
    }

    /**
     * Constructs a CashAccount with explicit derived current balance and archival flag.
     *
     * @param id             unique account ID
     * @param name           account name
     * @param openingBalance initial opening balance
     * @param currentBalance derived current balance
     * @param archived       true if archived
     */
    public CashAccount(
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
     * Constructs an active CashAccount.
     *
     * @param id             unique account ID
     * @param name           account name
     * @param openingBalance initial opening balance
     */
    public CashAccount(Long id, String name, Money openingBalance) {
        super(id, name, openingBalance);
        this.currentBalance = openingBalance;
    }

    @Override
    public String type() {
        return "CASH";
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
                    "Insufficient cash funds: requested " + amount + ", available " + currentBalance);
        }
        this.currentBalance = this.currentBalance.subtract(amount);
    }

    @Override
    public void credit(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        this.currentBalance = this.currentBalance.add(amount);
    }
}
