package piems.domain;

import java.time.LocalDate;

/**
 * Transaction representing an expenditure debited from an account.
 *
 * <p>Implements Requirement FR-03. Reduces account balance via {@link #effectOn(Account)}
 * and enforces debit constraints via {@link #applyTo(Account)}.</p>
 */
public class Expense extends Transaction {

    /**
     * Constructs an Expense transaction with category component.
     *
     * @param id       unique transaction ID, or null if unpersisted
     * @param date     transaction date
     * @param amount   positive monetary expense amount
     * @param note     optional note
     * @param account  source account debited
     * @param category associated expense category component
     */
    public Expense(
            Long id, LocalDate date, Money amount, String note, Account account, CategoryComponent category) {
        super(id, date, amount, note, account, category);
    }

    /**
     * Constructs an Expense transaction.
     *
     * @param id         unique transaction ID, or null if unpersisted
     * @param date       transaction date
     * @param amount     positive monetary expense amount
     * @param note       optional note
     * @param account    source account debited
     * @param categoryId optional associated expense category ID
     */
    public Expense(Long id, LocalDate date, Money amount, String note, Account account, Long categoryId) {
        super(id, date, amount, note, account, categoryId);
    }

    @Override
    public String kind() {
        return "EXPENSE";
    }

    @Override
    public long effectOn(Account target) {
        if (target != null && target.equals(account())) {
            return -amount().minorUnits();
        }
        return 0L;
    }

    @Override
    public void applyTo(Account target) throws InsufficientFundsException {
        if (target != null && target.equals(account())) {
            target.debit(amount());
        }
    }

    @Override
    public void reverseOn(Account target) {
        if (target != null && target.equals(account())) {
            target.credit(amount());
        }
    }
}
