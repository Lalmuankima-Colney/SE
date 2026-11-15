package piems.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit and contract tests for {@link CashAccount}.
 *
 * <p>Verifies physical cash account rules: zero overdraft (Decision Table R1, R2),
 * initial opening balance derivation (TC-FR01-01), expense within balance (TC-FR03-01),
 * and expense rejection above balance (TC-FR03-02).</p>
 */
public class CashAccountTest extends AccountContractTest {

    @Override
    protected Account createAccount(String name, Money openingOrLimit) {
        return new CashAccount(1L, name, openingOrLimit);
    }

    @Override
    protected Account createAccount(String name, Money openingOrLimit, boolean archived) {
        return new CashAccount(1L, name, openingOrLimit, archived);
    }

    @Test
    void initialBalanceMatchesOpening() {
        // TC-FR01-01: Create cash account with opening balance 50.00 -> balance is 50.00
        CashAccount account = new CashAccount(10L, "Wallet", Money.of("50.00"));
        assertEquals(Money.of("50.00"), account.balance());
        assertEquals(Money.of("50.00"), account.openingBalance());
        assertEquals(10L, account.id());
        assertFalse(account.isArchived());
    }

    @Test
    void acceptsExpenseWithinBalance() throws InsufficientFundsException {
        // TC-FR03-01, Decision Table Row R1: Cash balance 100.00, record expense 40.00 -> balance is 60.00
        CashAccount account = new CashAccount(1L, "Pocket", Money.of("100.00"));
        assertTrue(account.canDebit(Money.of("40.00")));
        account.debit(Money.of("40.00"));
        assertEquals(Money.of("60.00"), account.balance());
    }

    @Test
    void rejectsExpenseAboveBalance() {
        // TC-FR03-02, Decision Table Row R2: Cash balance 100.00, expense 100.01 -> Insufficient-funds error
        CashAccount account = new CashAccount(1L, "Pocket", Money.of("100.00"));
        assertFalse(account.canDebit(Money.of("100.01")));
        InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class,
                () -> account.debit(Money.of("100.01")));
        assertTrue(ex.getMessage().contains("Insufficient cash funds"));
        assertEquals(Money.of("100.00"), account.balance());
    }

    @Test
    void exactBalanceDebitLeavesZero() throws InsufficientFundsException {
        CashAccount account = new CashAccount(1L, "Pocket", Money.of("50.00"));
        assertTrue(account.canDebit(Money.of("50.00")));
        account.debit(Money.of("50.00"));
        assertEquals(Money.of("0.00"), account.balance());
    }

    @Test
    void creditIncreasesBalance() {
        CashAccount account = new CashAccount(1L, "Pocket", Money.of("50.00"));
        account.credit(Money.of("25.00"));
        assertEquals(Money.of("75.00"), account.balance());
    }

    @Test
    void constructorRejectsInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> new CashAccount(1L, null, Money.of("10.00")));
        assertThrows(IllegalArgumentException.class, () -> new CashAccount(1L, "   ", Money.of("10.00")));
    }
}
