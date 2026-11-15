package piems.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abstract contract test verifying polymorphic behavior across all Account subclasses.
 *
 * <p>Enforces the Liskov substitution principle (LSP) and verifies common invariants
 * including debit rejection when archived (Decision Table Row R5, TC-FR01-02),
 * history preservation upon archiving (TC-FR01-01), null validations,
 * and debit evaluation contracts (Decision D-07).</p>
 */
public abstract class AccountContractTest {

    /**
     * Factory method creating an active account with the given name and capacity.
     *
     * @param name           account name
     * @param openingOrLimit monetary opening balance or credit limit
     * @return concrete Account instance
     */
    protected abstract Account createAccount(String name, Money openingOrLimit);

    /**
     * Factory method creating an account with explicit archival status.
     *
     * @param name           account name
     * @param openingOrLimit monetary opening balance or credit limit
     * @param archived       archival flag
     * @return concrete Account instance
     */
    protected abstract Account createAccount(String name, Money openingOrLimit, boolean archived);

    @Test
    void archivedAccountRejectsAnyDebit() {
        // TC-FR01-02, Decision Table Row R5
        Account account = createAccount("Archived Test", Money.of("100.00"), true);
        assertTrue(account.isArchived());
        assertFalse(account.canDebit(Money.of("10.00")));
        assertThrows(InsufficientFundsException.class, () -> account.debit(Money.of("10.00")));
    }

    @Test
    void dynamicArchivingRejectsSubsequentDebits() throws InsufficientFundsException {
        // Decision Table Row R5
        Account account = createAccount("Dynamic Archive", Money.of("100.00"));
        assertFalse(account.isArchived());
        assertTrue(account.canDebit(Money.of("20.00")));

        account.archive();
        assertTrue(account.isArchived());
        assertFalse(account.canDebit(Money.of("20.00")));
        assertThrows(InsufficientFundsException.class, () -> account.debit(Money.of("20.00")));
    }

    @Test
    void keepsHistoryWhenArchived() {
        // TC-FR01-01, TC-FR01-02
        Account account = createAccount("History Account", Money.of("150.00"));
        assertEquals("History Account", account.name());
        Money opening = account.openingBalance();

        account.archive();
        assertTrue(account.isArchived());
        assertEquals("History Account", account.name());
        assertEquals(opening, account.openingBalance());
    }

    @Test
    void acceptsExpenseWithinBalanceOrLimit() throws InsufficientFundsException {
        Account account = createAccount("Accept Debit", Money.of("100.00"));
        assertTrue(account.canDebit(Money.of("40.00")));
        account.debit(Money.of("40.00"));
    }

    @Test
    void rejectsExpenseAboveBalanceOrLimit() {
        Account account = createAccount("Reject Overdraft", Money.of("100.00"));
        assertFalse(account.canDebit(Money.of("100.01")));
        assertThrows(InsufficientFundsException.class, () -> account.debit(Money.of("100.01")));
    }

    @Test
    void rejectsNullInputs() {
        Account account = createAccount("Null Checks", Money.of("100.00"));
        assertThrows(NullPointerException.class, () -> account.canDebit(null));
        assertThrows(NullPointerException.class, () -> account.debit(null));
        assertThrows(NullPointerException.class, () -> account.credit(null));
    }

    @Test
    void exceptionPreservesMessageAndCause() {
        RuntimeException cause = new RuntimeException("Underlying failure");
        InsufficientFundsException ex = new InsufficientFundsException("Debit failed", cause);
        assertEquals("Debit failed", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
