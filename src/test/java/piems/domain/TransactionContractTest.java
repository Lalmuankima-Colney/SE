package piems.domain;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abstract contract test verifying common behavioral invariants across all Transaction subclasses.
 *
 * <p>Validates positive monetary amount invariants (FR-02, FR-03), date/account retention,
 * ceiling limits, and zero effect on unrelated accounts.</p>
 */
public abstract class TransactionContractTest {

    /** Common deterministic test date. */
    protected static final LocalDate TEST_DATE = LocalDate.of(2026, 11, 14);

    /**
     * Creates an account for transaction testing.
     *
     * @param name    account name
     * @param opening initial opening money
     * @return concrete account
     */
    protected Account createTestAccount(String name, Money opening) {
        return new CashAccount(1L, name, opening);
    }

    /**
     * Factory method creating a concrete transaction subtype under test.
     *
     * @param amount  monetary amount
     * @param account associated primary account
     * @return concrete transaction instance
     */
    protected abstract Transaction createTransaction(Money amount, Account account);

    @Test
    void maintainsMetadataAndPositiveAmount() {
        Account account = createTestAccount("Main", Money.of("100.00"));
        Transaction tx = createTransaction(Money.of("50.00"), account);

        assertNotNull(tx.kind());
        assertEquals(TEST_DATE, tx.date());
        assertEquals(Money.of("50.00"), tx.amount());
        assertEquals(account, tx.account());
        assertTrue(tx.amount().minorUnits() > 0);
    }

    @Test
    void rejectsZeroAmount() {
        Account account = createTestAccount("Main", Money.of("100.00"));
        assertThrows(IllegalArgumentException.class, () -> createTransaction(Money.of("0.00"), account));
    }

    @Test
    void rejectsAmountExceedingMaximumCeiling() {
        Account account = createTestAccount("Main", Money.of("100.00"));
        assertThrows(IllegalArgumentException.class, () -> createTransaction(Money.ofMinor(100_000_000_000L), account));
    }

    @Test
    void effectOnUnrelatedAccountIsZero() {
        Account account = createTestAccount("Account 1", Money.of("100.00"));
        Account other = createTestAccount("Other", Money.of("100.00"));
        Transaction tx = createTransaction(Money.of("25.00"), account);

        assertEquals(0L, tx.effectOn(other));
        assertEquals(0L, tx.effectOn(null));
    }

    @Test
    void rejectsNullDateOrAccount() {
        Account acc = createTestAccount("A", Money.zero());
        assertThrows(NullPointerException.class, () -> new Income(1L, null, Money.of("10.00"), "n", acc, 1L));
        assertThrows(NullPointerException.class, () -> new Income(1L, TEST_DATE, Money.of("10.00"), "n", null, 1L));
        assertThrows(NullPointerException.class, () -> new Income(1L, TEST_DATE, null, "n", acc, 1L));
    }

    @Test
    void commonEqualsAndIdContract() {
        Account account = createTestAccount("Main", Money.of("100.00"));
        Transaction tx = createTransaction(Money.of("50.00"), account);

        // Reflexive
        assertEquals(tx, tx);
        assertNotNull(tx.toString());
        assertNotNull(tx.id());

        // Unpersisted hashCode
        Transaction unpersisted = new Income(null, TEST_DATE, Money.of("10.00"), "note", account, 1L);
        assertNotNull(unpersisted.hashCode());
    }

    @Test
    void constructorsRetainNullNote() {
        // TC-FR19-12
        Account account = createTestAccount("Acc", Money.of("100.00"));
        CategoryComponent cat = new Category(1L, "Food", "EXPENSE");
        Transaction tx1 = new Expense(null, TEST_DATE, Money.of("10.00"), null, account, cat);
        Transaction tx2 = new Expense(null, TEST_DATE, Money.of("10.00"), null, account, 1L);
        assertNull(tx1.note());
        assertNull(tx2.note());
    }

    @Test
    void unpersistedTransactionsAreEqualWhenAllAttributesMatch() {
        // TC-FR06-03
        Account account = createTestAccount("Acc", Money.of("100.00"));
        Transaction tx1 = new Expense(null, TEST_DATE, Money.of("15.00"), "Lunch", account, 2L);
        Transaction tx2 = new Expense(null, TEST_DATE, Money.of("15.00"), "Lunch", account, 2L);
        assertEquals(tx1, tx2);
        assertEquals(tx1.hashCode(), tx2.hashCode());
    }

    @Test
    void unpersistedTransactionsDifferWhenAnyAttributeDiffers() {
        // TC-FR06-04
        Account a1 = createTestAccount("Acc1", Money.of("100.00"));
        Account a2 = createTestAccount("Acc2", Money.of("100.00"));
        Transaction base = new Expense(null, TEST_DATE, Money.of("15.00"), "Lunch", a1, 2L);

        assertNotEquals(base, new Expense(null, TEST_DATE.plusDays(1), Money.of("15.00"), "Lunch", a1, 2L));
        assertNotEquals(base, new Expense(null, TEST_DATE, Money.of("16.00"), "Lunch", a1, 2L));
        assertNotEquals(base, new Expense(null, TEST_DATE, Money.of("15.00"), "Lunch", a2, 2L));
        assertNotEquals(base, new Expense(null, TEST_DATE, Money.of("15.00"), "Dinner", a1, 2L));
        assertNotEquals(base, new Expense(null, TEST_DATE, Money.of("15.00"), "Lunch", a1, 3L));
    }

    @Test
    void transactionEqualsComparesValuesWhenIdIsNull() {
        // TC-FR06-05
        Account account = createTestAccount("Acc", Money.of("100.00"));
        Transaction unpersisted = new Expense(null, TEST_DATE, Money.of("15.00"), "Lunch", account, 2L);
        Transaction persistedSame = new Expense(99L, TEST_DATE, Money.of("15.00"), "Lunch", account, 2L);
        Transaction persistedDifferent = new Expense(99L, TEST_DATE, Money.of("20.00"), "Dinner", account, 2L);

        assertEquals(unpersisted, persistedSame);
        assertNotEquals(unpersisted, persistedDifferent);
        assertNotEquals(persistedDifferent, unpersisted);
    }
}
