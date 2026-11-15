package piems.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit and contract tests for {@link Income}.
 *
 * <p>Verifies Requirement FR-02: raising account balance (TC-FR02-01),
 * rejecting non-positive amounts (TC-FR02-02), and balance effect calculations.</p>
 */
public class IncomeTest extends TransactionContractTest {

    @Override
    protected Transaction createTransaction(Money amount, Account account) {
        return new Income(1L, TEST_DATE, amount, "Salary", account, 10L);
    }

    @Test
    void incomeRaisesBalance() throws InsufficientFundsException {
        // TC-FR02-01: Account balance 0.00 -> record income 100.00 -> balance is 100.00
        Account account = createTestAccount("Wallet", Money.of("0.00"));
        Income income = new Income(1L, TEST_DATE, Money.of("100.00"), "Bonus", account, 10L);

        account.apply(income);
        assertEquals(Money.of("100.00"), account.balance());
        assertEquals(10000L, income.effectOn(account));
    }

    @Test
    void incomeRejectsNonPositiveAmount() {
        // TC-FR02-02: Any account -> record income 0.00 -> validation error, balance unchanged
        Account account = createTestAccount("Checking", Money.of("50.00"));
        assertThrows(IllegalArgumentException.class, () ->
                new Income(1L, TEST_DATE, Money.of("0.00"), "Zero", account, 10L));
        assertEquals(Money.of("50.00"), account.balance());
    }

    @Test
    void effectOnSourceAccountIsPositive() {
        Account account = createTestAccount("Main", Money.of("50.00"));
        Income income = new Income(1L, TEST_DATE, Money.of("75.00"), "Freelance", account, 10L);

        assertEquals("INCOME", income.kind());
        assertEquals(7500L, income.effectOn(account));
        assertEquals(10L, income.categoryId());
        assertEquals("Freelance", income.note());
    }

    @Test
    void applyAndReverseKeepBalanceConsistent() throws InsufficientFundsException {
        Account account = createTestAccount("Savings", Money.of("100.00"));
        Income income = new Income(1L, TEST_DATE, Money.of("50.00"), "Dividends", account, 10L);

        income.applyTo(account);
        assertEquals(Money.of("150.00"), account.balance());

        income.reverseOn(account);
        assertEquals(Money.of("100.00"), account.balance());
    }

    @Test
    void equalsAndHashCodeContract() {
        Account a1 = createTestAccount("A1", Money.zero());
        Account a2 = createTestAccount("A2", Money.zero());
        Income inc1 = new Income(10L, TEST_DATE, Money.of("10.00"), "Note", a1, 1L);
        Income inc2 = new Income(10L, TEST_DATE, Money.of("10.00"), "Note", a1, 1L);
        Income inc3 = new Income(11L, TEST_DATE, Money.of("10.00"), "Note", a2, 1L);

        assertEquals(inc1, inc2);
        assertEquals(inc1.hashCode(), inc2.hashCode());
        assertNotEquals(inc1, inc3);
        assertNotEquals(inc1, null);
    }

    @Test
    void applyToDoesNotModifyForeignAccountBalance() {
        // TC-FR02-03, D-41
        Account primary = createTestAccount("Primary", Money.of("50.00"));
        Account foreign = createTestAccount("Foreign", Money.of("50.00"));
        Income income = new Income(1L, TEST_DATE, Money.of("25.00"), "Bonus", primary, 10L);

        income.applyTo(foreign);
        assertEquals(Money.of("50.00"), foreign.balance());
    }

    @Test
    void applyToWithNullTargetDoesNotThrow() {
        // TC-FR02-04, D-41
        Account primary = createTestAccount("Primary", Money.of("50.00"));
        Income income = new Income(1L, TEST_DATE, Money.of("25.00"), "Bonus", primary, 10L);

        income.applyTo(null);
        assertEquals(Money.of("50.00"), primary.balance());
    }

    @Test
    void reverseOnDoesNotModifyForeignAccountBalance() throws InsufficientFundsException {
        // TC-FR06-08, D-41
        Account primary = createTestAccount("Primary", Money.of("50.00"));
        Account foreign = createTestAccount("Foreign", Money.of("50.00"));
        Income income = new Income(1L, TEST_DATE, Money.of("25.00"), "Bonus", primary, 10L);

        income.reverseOn(foreign);
        assertEquals(Money.of("50.00"), foreign.balance());
    }

    @Test
    void reverseOnWithNullTargetDoesNotThrow() throws InsufficientFundsException {
        // TC-FR06-09, D-41
        Account primary = createTestAccount("Primary", Money.of("50.00"));
        Income income = new Income(1L, TEST_DATE, Money.of("25.00"), "Bonus", primary, 10L);

        income.reverseOn(null);
        assertEquals(Money.of("50.00"), primary.balance());
    }
}
