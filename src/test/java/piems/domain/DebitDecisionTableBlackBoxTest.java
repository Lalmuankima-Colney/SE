package piems.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box test suite for Debit Decision Table Rules R1 through R5.
 *
 * <p>Sources of truth: docs/TEST_PLAN.md Section 9.2, docs/SRS.md FR-01 and FR-03,
 * and Decision D-27 (zero overdraft on cash/bank) and D-31 (credit card credit limit).</p>
 */
public class DebitDecisionTableBlackBoxTest {

    /**
     * Rule R1: Cash or bank account with debit amount within balance is accepted.
     * TC-FR03-09: Cash opening 100.00, debit 40.00 -> balance 60.00;
     * Bank opening 200.00, debit 200.00 -> balance 0.00.
     */
    @Test
    @DisplayName("TC-FR03-09: R1 cash or bank debit within balance is accepted")
    public void r1CashOrBankDebitWithinBalanceIsAccepted() {
        CashAccount cash = new CashAccount(1L, "Cash Wallet", Money.of("100.00"));
        assertTrue(cash.canDebit(Money.of("40.00")));
        assertDoesNotThrow(() -> cash.debit(Money.of("40.00")));
        assertEquals(Money.of("60.00"), cash.balance());

        BankAccount bank = new BankAccount(2L, "Checking Account", Money.of("200.00"));
        assertTrue(bank.canDebit(Money.of("200.00")));
        assertDoesNotThrow(() -> bank.debit(Money.of("200.00")));
        assertEquals(Money.of("0.00"), bank.balance());
    }

    /**
     * Rule R2: Cash or bank account with debit amount exceeding balance throws InsufficientFundsException.
     * TC-FR03-10: Cash opening 50.00, debit 50.01 -> rejected, balance stays 50.00;
     * Bank opening 0.00, debit 0.01 -> rejected, balance stays 0.00.
     */
    @Test
    @DisplayName("TC-FR03-10: R2 cash or bank debit exceeding balance throws InsufficientFundsException")
    public void r2CashOrBankDebitExceedingBalanceThrowsInsufficientFunds() {
        CashAccount cash = new CashAccount(1L, "Cash Wallet", Money.of("50.00"));
        assertFalse(cash.canDebit(Money.of("50.01")));
        assertThrows(InsufficientFundsException.class, () -> cash.debit(Money.of("50.01")));
        assertEquals(Money.of("50.00"), cash.balance());

        BankAccount bank = new BankAccount(2L, "Savings Account", Money.of("0.00"));
        assertFalse(bank.canDebit(Money.of("0.01")));
        assertThrows(InsufficientFundsException.class, () -> bank.debit(Money.of("0.01")));
        assertEquals(Money.of("0.00"), bank.balance());
    }

    /**
     * Rule R3: Credit card account with debit amount within credit limit is accepted.
     * TC-FR03-11: Credit card limit 500.00, debt 0.00, debit 500.00 -> balance becomes -500.00.
     */
    @Test
    @DisplayName("TC-FR03-11: R3 credit card debit within credit limit is accepted")
    public void r3CreditCardDebitWithinCreditLimitIsAccepted() {
        CreditCardAccount card = new CreditCardAccount(3L, "Visa Card", Money.of("500.00"), false);
        assertTrue(card.canDebit(Money.of("500.00")));
        assertDoesNotThrow(() -> card.debit(Money.of("500.00")));
        assertEquals(Money.of("500.00"), card.outstandingDebt());
        assertEquals(Money.of("0.00"), card.availableCredit());
        assertEquals(Money.of("0.00"), card.balance());
    }

    /**
     * Rule R4: Credit card account with debit amount beyond credit limit throws InsufficientFundsException.
     * TC-FR03-12: Credit card limit 500.00, debt 0.00, debit 500.01 -> rejected; balance stays 0.00.
     */
    @Test
    @DisplayName("TC-FR03-12: R4 credit card debit exceeding credit limit throws InsufficientFundsException")
    public void r4CreditCardDebitExceedingCreditLimitThrowsInsufficientFunds() {
        CreditCardAccount card = new CreditCardAccount(3L, "Visa Card", Money.of("500.00"), false);
        assertFalse(card.canDebit(Money.of("500.01")));
        assertThrows(InsufficientFundsException.class, () -> card.debit(Money.of("500.01")));
        assertEquals(Money.of("0.00"), card.balance());
        assertEquals(Money.of("500.00"), card.availableCredit());
    }

    /**
     * Rule R5: Archived account of any type rejects debit regardless of balance or limit.
     * TC-FR01-03: Cash balance 1000.00 archived -> debit 1.00 throws InsufficientFundsException;
     * Credit card limit 1000.00 archived -> debit 1.00 throws InsufficientFundsException.
     */
    @Test
    @DisplayName("TC-FR01-03: R5 archived account rejects debit regardless of balance")
    public void r5ArchivedAccountRejectsDebitRegardlessOfBalance() {
        CashAccount archivedCash = new CashAccount(1L, "Archived Cash", Money.of("1000.00"), true);
        assertFalse(archivedCash.canDebit(Money.of("1.00")));
        assertThrows(InsufficientFundsException.class, () -> archivedCash.debit(Money.of("1.00")));
        assertEquals(Money.of("1000.00"), archivedCash.balance());

        CreditCardAccount archivedCard = new CreditCardAccount(
                2L, "Archived Card", Money.of("1000.00"), true);
        assertFalse(archivedCard.canDebit(Money.of("1.00")));
        assertThrows(InsufficientFundsException.class, () -> archivedCard.debit(Money.of("1.00")));
        assertEquals(Money.of("0.00"), archivedCard.balance());
    }
}
