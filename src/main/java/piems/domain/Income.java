package piems.domain;

import java.time.LocalDate;

/**
 * Transaction representing incoming funds credited to an account.
 *
 * <p>Implements Requirement FR-02. Raises account balance via {@link #effectOn(Account)}
 * and {@link #applyTo(Account)}.</p>
 */
public class Income extends Transaction {

    /**
     * Constructs an Income transaction with category component.
     *
     * @param id       unique transaction ID, or null if unpersisted
     * @param date     transaction date
     * @param amount   positive monetary income amount
     * @param note     optional note
     * @param account  destination account receiving income
     * @param category associated income category component
     */
    public Income(
            Long id, LocalDate date, Money amount, String note, Account account, CategoryComponent category) {
        super(id, date, amount, note, account, category);
    }

    /**
     * Constructs an Income transaction.
     *
     * @param id         unique transaction ID, or null if unpersisted
     * @param date       transaction date
     * @param amount     positive monetary income amount
     * @param note       optional note
     * @param account    destination account receiving income
     * @param categoryId optional associated income category ID
     */
    public Income(Long id, LocalDate date, Money amount, String note, Account account, Long categoryId) {
        super(id, date, amount, note, account, categoryId);
    }

    @Override
    public String kind() {
        return "INCOME";
    }

    @Override
    public long effectOn(Account target) {
        if (target != null && target.equals(account())) {
            return amount().minorUnits();
        }
        return 0L;
    }

    @Override
    public void applyTo(Account target) {
        if (target != null && target.equals(account())) {
            target.credit(amount());
        }
    }

    @Override
    public void reverseOn(Account target) throws InsufficientFundsException {
        if (target != null && target.equals(account())) {
            target.debit(amount());
        }
    }
}
