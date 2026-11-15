package piems.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit and contract tests for {@link BankAccount}.
 *
 * <p>Verifies bank account rules: overdraft prohibited in v1.0 (Decision D-27, Q-001),
 * expense rejection above balance (TC-FR03-04, Decision Table Row R2),
 * debit within balance (Decision Table Row R1), and balance integrity.</p>
 */
public class BankAccountTest extends AccountContractTest {

    @Override
    protected Account createAccount(String name, Money openingOrLimit) {
        return new BankAccount(2L, name, openingOrLimit);
    }

    @Override
    protected Account createAccount(String name, Money openingOrLimit, boolean archived) {
        return new BankAccount(2L, name, openingOrLimit, archived);
    }

    @Test
    void initialBalanceMatchesOpening() {
        BankAccount account = new BankAccount(20L, "Checking", Money.of("200.00"));
        assertEquals(Money.of("200.00"), account.balance());
        assertEquals(Money.of("200.00"), account.openingBalance());
        assertEquals(20L, account.id());
        assertFalse(account.isArchived());
    }

    @Test
    void bankAcceptsDebitWithinBalance() throws InsufficientFundsException {
        // Decision Table Row R1: Bank balance 100.00, record expense 40.00 -> balance is 60.00
        BankAccount account = new BankAccount(2L, "Checking", Money.of("100.00"));
        assertTrue(account.canDebit(Money.of("40.00")));
        account.debit(Money.of("40.00"));
        assertEquals(Money.of("60.00"), account.balance());
    }

    @Test
    void bankRejectsExpenseAboveBalanceWhenNoOverdraft() {
        // TC-FR03-04, Decision Table Row R2, Decision D-27:
        // Bank balance 100.00, record expense 100.01 -> InsufficientFundsException, balance unchanged
        BankAccount account = new BankAccount(2L, "Savings", Money.of("100.00"));
        assertFalse(account.canDebit(Money.of("100.01")));
        InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class,
                () -> account.debit(Money.of("100.01")));
        assertTrue(ex.getMessage().contains("overdraft prohibited in v1.0"));
        assertEquals(Money.of("100.00"), account.balance());
    }

    @Test
    void creditIncreasesBalance() {
        BankAccount account = new BankAccount(2L, "Checking", Money.of("100.00"));
        account.credit(Money.of("50.00"));
        assertEquals(Money.of("150.00"), account.balance());
    }
}
