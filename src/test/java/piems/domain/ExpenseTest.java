package piems.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit and contract tests for {@link Expense}.
 *
 * <p>Verifies Requirement FR-03: expense debiting, overdraft prevention,
 * negative balance effect, and edit consistency (TC-FR06-01).</p>
 */
public class ExpenseTest extends TransactionContractTest {

    @Override
    protected Transaction createTransaction(Money amount, Account account) {
        return new Expense(2L, TEST_DATE, amount, "Groceries", account, 20L);
    }

    @Test
    void acceptsExpenseWithinBalance() throws InsufficientFundsException {
        Account account = createTestAccount("Cash", Money.of("100.00"));
        Expense expense = new Expense(2L, TEST_DATE, Money.of("40.00"), "Lunch", account, 20L);

        account.apply(expense);
        assertEquals(Money.of("60.00"), account.balance());
        assertEquals(-4000L, expense.effectOn(account));
    }

    @Test
    void rejectsExpenseAboveBalance() {
        Account account = createTestAccount("Cash", Money.of("100.00"));
        Expense expense = new Expense(2L, TEST_DATE, Money.of("100.01"), "Overdraft", account, 20L);

        assertThrows(InsufficientFundsException.class, () -> account.apply(expense));
        assertEquals(Money.of("100.00"), account.balance());
    }

    @Test
    void effectOnSourceAccountIsNegative() {
        Account account = createTestAccount("Cash", Money.of("100.00"));
        Expense expense = new Expense(2L, TEST_DATE, Money.of("25.00"), "Transport", account, 20L);

        assertEquals("EXPENSE", expense.kind());
        assertEquals(-2500L, expense.effectOn(account));
        assertEquals(20L, expense.categoryId());
        assertEquals("Transport", expense.note());
    }

    @Test
    void editKeepsBalancesConsistent() throws InsufficientFundsException {
        // TC-FR06-01: Expense 40.00 recorded -> edit amount to 30.00 -> balance is 10.00 higher
        Account account = createTestAccount("Checking", Money.of("100.00"));
        Expense originalExpense = new Expense(2L, TEST_DATE, Money.of("40.00"), "Dinner", account, 20L);

        account.apply(originalExpense);
        assertEquals(Money.of("60.00"), account.balance());

        // Edit amount from 40.00 to 30.00
        account.reverse(originalExpense);
        Expense editedExpense = new Expense(2L, TEST_DATE, Money.of("30.00"), "Dinner (discounted)", account, 20L);
        account.apply(editedExpense);

        assertEquals(Money.of("70.00"), account.balance());
    }

    @Test
    void equalsAndHashCodeContract() {
        Account a1 = createTestAccount("A1", Money.zero());
        Expense exp1 = new Expense(20L, TEST_DATE, Money.of("20.00"), "Dinner", a1, 2L);
        Expense exp2 = new Expense(20L, TEST_DATE, Money.of("20.00"), "Dinner", a1, 2L);
        Expense exp3 = new Expense(21L, TEST_DATE, Money.of("20.00"), "Dinner", a1, 2L);

        assertEquals(exp1, exp2);
        assertEquals(exp1.hashCode(), exp2.hashCode());
        assertNotEquals(exp1, exp3);
    }

    @Test
    void applyToDoesNotModifyForeignAccountBalance() throws InsufficientFundsException {
        // TC-FR03-07, D-41
        Account primary = createTestAccount("Primary", Money.of("100.00"));
        Account foreign = createTestAccount("Foreign", Money.of("100.00"));
        Expense expense = new Expense(2L, TEST_DATE, Money.of("30.00"), "Lunch", primary, 20L);

        expense.applyTo(foreign);
        assertEquals(Money.of("100.00"), foreign.balance());
    }

    @Test
    void applyToWithNullTargetDoesNotThrow() throws InsufficientFundsException {
        // TC-FR03-08, D-41
        Account primary = createTestAccount("Primary", Money.of("100.00"));
        Expense expense = new Expense(2L, TEST_DATE, Money.of("30.00"), "Lunch", primary, 20L);

        expense.applyTo(null);
        assertEquals(Money.of("100.00"), primary.balance());
    }

    @Test
    void reverseOnDoesNotModifyForeignAccountBalance() {
        // TC-FR06-06, D-41
        Account primary = createTestAccount("Primary", Money.of("100.00"));
        Account foreign = createTestAccount("Foreign", Money.of("100.00"));
        Expense expense = new Expense(2L, TEST_DATE, Money.of("30.00"), "Lunch", primary, 20L);

        expense.reverseOn(foreign);
        assertEquals(Money.of("100.00"), foreign.balance());
    }

    @Test
    void reverseOnWithNullTargetDoesNotThrow() {
        // TC-FR06-07, D-41
        Account primary = createTestAccount("Primary", Money.of("100.00"));
        Expense expense = new Expense(2L, TEST_DATE, Money.of("30.00"), "Lunch", primary, 20L);

        expense.reverseOn(null);
        assertEquals(Money.of("100.00"), primary.balance());
    }
}
