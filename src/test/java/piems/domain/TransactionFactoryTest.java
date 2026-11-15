package piems.domain;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TransactionFactory}.
 *
 * <p>Verifies boundary values (TC-FR19-01), input equivalence classes,
 * validation errors, and subtype instantiation according to Requirement FR-19.</p>
 */
public class TransactionFactoryTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2026, 11, 14);

    private Account createAccount(String name) {
        return new CashAccount(1L, name, Money.of("100.00"));
    }

    @Test
    void createsIncomeSubtypeFromString() throws ValidationException {
        Account account = createAccount("Cash");
        Transaction tx = TransactionFactory.createFromString(
                "INCOME", "2026-11-14", "50.00", "Gift", account, null, 1L);

        assertTrue(tx instanceof Income);
        assertEquals("INCOME", tx.kind());
        assertEquals(Money.of("50.00"), tx.amount());
        assertEquals(5000L, tx.effectOn(account));
    }

    @Test
    void createsExpenseSubtypeFromString() throws ValidationException {
        Account account = createAccount("Cash");
        Transaction tx = TransactionFactory.createFromString(
                "expense", "2026-11-14", "25.00", "Coffee", account, null, 2L);

        assertTrue(tx instanceof Expense);
        assertEquals("EXPENSE", tx.kind());
        assertEquals(Money.of("25.00"), tx.amount());
        assertEquals(-2500L, tx.effectOn(account));
    }

    @Test
    void createsTransferSubtypeFromString() throws ValidationException {
        Account a = createAccount("A");
        Account b = createAccount("B");
        Transaction tx = TransactionFactory.createFromString(
                "TRANSFER", "2026-11-14", "30.00", "Move", a, b, null);

        assertTrue(tx instanceof Transfer);
        assertEquals("TRANSFER", tx.kind());
        assertEquals(-3000L, tx.effectOn(a));
        assertEquals(3000L, tx.effectOn(b));
    }

    @Test
    void boundaryAmountZeroRejected() {
        // TC-FR19-01 boundary: 0.00 rejected
        Account account = createAccount("Cash");
        ValidationException ex = assertThrows(ValidationException.class, () ->
                TransactionFactory.createFromString("INCOME", "2026-11-14", "0.00", "Zero", account, null, 1L));
        assertTrue(ex.getMessage().contains("must be strictly positive"));
    }

    @Test
    void boundaryAmountMinAccepted() throws ValidationException {
        // TC-FR19-01 boundary: 0.01 accepted
        Account account = createAccount("Cash");
        Transaction tx = TransactionFactory.createFromString(
                "INCOME", "2026-11-14", "0.01", "Penny", account, null, 1L);
        assertEquals(Money.of("0.01"), tx.amount());
    }

    @Test
    void boundaryAmountMaxAccepted() throws ValidationException {
        // TC-FR19-01 boundary: 999,999,999.99 accepted
        Account account = createAccount("Cash");
        Transaction tx = TransactionFactory.createFromString(
                "INCOME", "2026-11-14", "999999999.99", "Ceiling", account, null, 1L);
        assertEquals(Money.of("999999999.99"), tx.amount());
    }

    @Test
    void boundaryAmountAboveMaxRejected() {
        // TC-FR19-01 boundary: 1,000,000,000.00 rejected
        Account account = createAccount("Cash");
        ValidationException ex = assertThrows(ValidationException.class, () ->
                TransactionFactory.createFromString(
                        "INCOME", "2026-11-14", "1000000000.00", "Above Ceiling", account, null, 1L));
        assertTrue(ex.getMessage().contains("exceeds ceiling"));
    }

    @Test
    void boundaryAmountTooManyDecimalsRejected() {
        // TC-FR19-01 boundary: 0.001 rejected
        Account account = createAccount("Cash");
        ValidationException ex = assertThrows(ValidationException.class, () ->
                TransactionFactory.createFromString("INCOME", "2026-11-14", "0.001", "Sub-cent", account, null, 1L));
        assertTrue(ex.getMessage().contains("Invalid monetary amount"));
    }

    @Test
    void rejectsUnknownOrBlankKind() {
        Account account = createAccount("Cash");
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createFromString("DIVIDEND", "2026-11-14", "10.00", "Note", account, null, 1L));
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createFromString("  ", "2026-11-14", "10.00", "Note", account, null, 1L));
    }

    @Test
    void rejectsInvalidDateOrMissingAccount() {
        Account account = createAccount("Cash");
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createFromString("INCOME", "bad-date", "10.00", "Note", account, null, 1L));
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createFromString("INCOME", "2026-11-14", "10.00", "Note", null, null, 1L));
    }

    @Test
    void rejectsInvalidTransferConfigurations() {
        Account a = createAccount("A");
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createFromString("TRANSFER", "2026-11-14", "10.00", "Note", a, null, null));
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createFromString("TRANSFER", "2026-11-14", "10.00", "Note", a, a, null));
    }

    @Test
    void directSubtypeFactoryMethods() {
        Account a = createAccount("A");
        Account b = createAccount("B");

        Income inc = TransactionFactory.createIncome(1L, TEST_DATE, Money.of("10.00"), "Inc", a, 1L);
        Expense exp = TransactionFactory.createExpense(2L, TEST_DATE, Money.of("20.00"), "Exp", a, 2L);
        Transfer tr = TransactionFactory.createTransfer(3L, TEST_DATE, Money.of("30.00"), "Tr", a, b, "uuid-1");

        assertNotNull(inc);
        assertNotNull(exp);
        assertNotNull(tr);
        assertEquals("uuid-1", tr.transferLinkId());
    }

    @Test
    void rejectsNullDateAndAmountInCreate() {
        Account a = createAccount("A");
        assertThrows(ValidationException.class, () ->
                TransactionFactory.create("INCOME", null, Money.of("10.00"), "note", a, null, 1L));
        assertThrows(ValidationException.class, () ->
                TransactionFactory.create("INCOME", TEST_DATE, null, "note", a, null, 1L));
    }

    @Test
    void rejectsNullOrBlankStringsInCreateFromString() {
        Account a = createAccount("A");
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createFromString("INCOME", null, "10.00", "note", a, null, 1L));
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createFromString("INCOME", "   ", "10.00", "note", a, null, 1L));
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createFromString("INCOME", "2026-11-14", null, "note", a, null, 1L));
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createFromString("INCOME", "2026-11-14", "   ", "note", a, null, 1L));
    }

    @Test
    void exceptionClassesPreserveMessageAndCause() {
        RuntimeException cause = new RuntimeException("Underlying reason");
        ValidationException ve = new ValidationException("Invalid input", cause);
        assertEquals("Invalid input", ve.getMessage());
        assertEquals(cause, ve.getCause());

        RecordNotFoundException rnfe1 = new RecordNotFoundException("Record 404");
        RecordNotFoundException rnfe2 = new RecordNotFoundException("Record 404", cause);
        assertEquals("Record 404", rnfe1.getMessage());
        assertEquals(cause, rnfe2.getCause());
    }

    @Test
    void createsIncomeWithCategoryComponent() throws ValidationException {
        // TC-FR19-04
        Account a = createAccount("Cash");
        CategoryComponent cat = new Category(5L, "Salary", "INCOME");
        Income inc = TransactionFactory.createIncome(1L, TEST_DATE, Money.of("100.00"), "Pay", a, cat);
        assertNotNull(inc);
        assertEquals(cat, inc.category());
        assertEquals(5L, inc.categoryId());
    }

    @Test
    void createsExpenseWithCategoryComponent() throws ValidationException {
        // TC-FR19-05
        Account a = createAccount("Cash");
        CategoryComponent cat = new Category(6L, "Food", "EXPENSE");
        Expense exp = TransactionFactory.createExpense(2L, TEST_DATE, Money.of("20.00"), "Lunch", a, cat);
        assertNotNull(exp);
        assertEquals(cat, exp.category());
        assertEquals(6L, exp.categoryId());
    }

    @Test
    void rejectsArchivedCategoryWhenCreatingIncome() {
        // TC-FR05-03
        Account a = createAccount("Cash");
        Category cat = new Category(5L, "OldSalary", "INCOME");
        cat.archive();
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createIncome(1L, TEST_DATE, Money.of("100.00"), "Pay", a, cat));
    }

    @Test
    void rejectsArchivedCategoryWhenCreatingExpense() {
        // TC-FR05-04
        Account a = createAccount("Cash");
        Category cat = new Category(6L, "OldFood", "EXPENSE");
        cat.archive();
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createExpense(2L, TEST_DATE, Money.of("20.00"), "Lunch", a, cat));
    }

    @Test
    void createsIncomeWithCategoryViaDispatcher() throws ValidationException {
        // TC-FR19-06
        Account a = createAccount("Cash");
        CategoryComponent cat = new Category(5L, "Bonus", "INCOME");
        Transaction tx = TransactionFactory.createWithCategory(
                "INCOME", TEST_DATE, Money.of("50.00"), "Bonus", a, null, cat);
        assertTrue(tx instanceof Income);
        assertEquals(cat, tx.category());
    }

    @Test
    void createsExpenseWithCategoryViaDispatcher() throws ValidationException {
        // TC-FR19-07
        Account a = createAccount("Cash");
        CategoryComponent cat = new Category(6L, "Dining", "EXPENSE");
        Transaction tx = TransactionFactory.createWithCategory(
                "EXPENSE", TEST_DATE, Money.of("30.00"), "Dinner", a, null, cat);
        assertTrue(tx instanceof Expense);
        assertEquals(cat, tx.category());
    }

    @Test
    void createsTransferWithCategoryViaDispatcher() throws ValidationException {
        // TC-FR19-08
        Account a = createAccount("Source");
        Account b = createAccount("Dest");
        Transaction tx = TransactionFactory.createWithCategory(
                "TRANSFER", TEST_DATE, Money.of("40.00"), "Transfer", a, b, null);
        assertTrue(tx instanceof Transfer);
        assertEquals(Money.of("40.00"), tx.amount());
    }

    @Test
    void rejectsUnknownKindInCreateWithCategory() {
        // TC-FR19-09
        Account a = createAccount("Cash");
        CategoryComponent cat = new Category(7L, "Misc", "INCOME");
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createWithCategory("DIVIDEND", TEST_DATE, Money.of("10.00"), "Div", a, null, cat));
    }

    @Test
    void createsFromStringWithCategoryComponent() throws ValidationException {
        // TC-FR19-10
        Account a = createAccount("Cash");
        CategoryComponent cat = new Category(8L, "Fuel", "EXPENSE");
        Transaction tx = TransactionFactory.createFromStringWithCategory(
                "EXPENSE", "2026-11-14", "45.00", "Gas", a, null, cat);
        assertTrue(tx instanceof Expense);
        assertEquals(cat, tx.category());
        assertEquals(Money.of("45.00"), tx.amount());
    }

    @Test
    void rejectsWhitespaceOnlyKind() {
        // TC-FR19-11
        Account a = createAccount("Cash");
        assertThrows(ValidationException.class, () ->
                TransactionFactory.createWithCategory("   ", TEST_DATE, Money.of("10.00"), "Note", a, null, null));
    }
}
