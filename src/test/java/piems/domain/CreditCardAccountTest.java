package piems.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit and contract tests for {@link CreditCardAccount}.
 *
 * <p>Verifies credit card debit rules (Decision Table R3, R4) and explicit debt tracking
 * via non-negative Money variables (Decision D-31). Covers TC-FR03-03.</p>
 */
public class CreditCardAccountTest extends AccountContractTest {

    @Override
    protected Account createAccount(String name, Money openingOrLimit) {
        return new CreditCardAccount(3L, name, openingOrLimit);
    }

    @Override
    protected Account createAccount(String name, Money openingOrLimit, boolean archived) {
        return new CreditCardAccount(3L, name, openingOrLimit, archived);
    }

    @Test
    void initialCardStateHasZeroDebtAndFullCredit() {
        CreditCardAccount card = new CreditCardAccount(30L, "Visa", Money.of("500.00"));
        assertEquals(Money.of("500.00"), card.creditLimit());
        assertEquals(Money.of("0.00"), card.outstandingDebt());
        assertEquals(Money.of("500.00"), card.availableCredit());
        assertEquals(Money.of("0.00"), card.balance());
        assertEquals(30L, card.id());
        assertFalse(card.isArchived());
    }

    @Test
    void creditCardAcceptsWithinLimit() throws InsufficientFundsException {
        // TC-FR03-03, Decision Table Row R3: Limit 500.00, expense 500.00 accepted
        CreditCardAccount card = new CreditCardAccount(3L, "Visa", Money.of("500.00"));
        assertTrue(card.canDebit(Money.of("500.00")));
        card.debit(Money.of("500.00"));
        assertEquals(Money.of("500.00"), card.outstandingDebt());
        assertEquals(Money.of("0.00"), card.availableCredit());
    }

    @Test
    void creditCardRejectsBeyondLimit() {
        // TC-FR03-03, Decision Table Row R4: Limit 500.00, expense 500.01 rejected
        CreditCardAccount card = new CreditCardAccount(3L, "Visa", Money.of("500.00"));
        assertFalse(card.canDebit(Money.of("500.01")));
        InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class,
                () -> card.debit(Money.of("500.01")));
        assertTrue(ex.getMessage().contains("Credit limit exceeded"));
        assertEquals(Money.of("0.00"), card.outstandingDebt());
        assertEquals(Money.of("500.00"), card.availableCredit());
    }

    @Test
    void multipleDebitsAccumulateDebtUntilLimit() throws InsufficientFundsException {
        CreditCardAccount card = new CreditCardAccount(3L, "Visa", Money.of("500.00"));
        card.debit(Money.of("300.00"));
        assertEquals(Money.of("300.00"), card.outstandingDebt());
        assertEquals(Money.of("200.00"), card.availableCredit());

        assertTrue(card.canDebit(Money.of("200.00")));
        assertFalse(card.canDebit(Money.of("200.01")));

        card.debit(Money.of("200.00"));
        assertEquals(Money.of("500.00"), card.outstandingDebt());
        assertEquals(Money.of("0.00"), card.availableCredit());
        assertFalse(card.canDebit(Money.of("0.01")));
    }

    @Test
    void creditReducesDebtAndRestoresAvailableCredit() throws InsufficientFundsException {
        CreditCardAccount card = new CreditCardAccount(3L, "Visa", Money.of("500.00"));
        card.debit(Money.of("300.00"));
        card.credit(Money.of("100.00"));
        assertEquals(Money.of("200.00"), card.outstandingDebt());
        assertEquals(Money.of("300.00"), card.availableCredit());
    }

    @Test
    void creditOverpaymentCapsDebtAtZero() throws InsufficientFundsException {
        CreditCardAccount card = new CreditCardAccount(3L, "Visa", Money.of("500.00"));
        card.debit(Money.of("50.00"));
        card.credit(Money.of("100.00"));
        assertEquals(Money.of("0.00"), card.outstandingDebt());
        assertEquals(Money.of("500.00"), card.availableCredit());
    }
}
