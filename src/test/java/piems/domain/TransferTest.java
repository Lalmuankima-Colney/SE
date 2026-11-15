package piems.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit and contract tests for {@link Transfer}.
 *
 * <p>Verifies Requirement FR-04 and Decision D-23: net worth invariant (TC-FR04-01),
 * two-leg debit/credit balance effects, shared link ID UUID, and atomic reversal.</p>
 */
public class TransferTest extends TransactionContractTest {

    @Override
    protected Transaction createTransaction(Money amount, Account account) {
        Account dest = createTestAccount("Dest", Money.of("100.00"));
        return new Transfer(3L, TEST_DATE, amount, "Transfer", account, dest, null);
    }

    @Test
    void transferKeepsNetWorth() throws InsufficientFundsException {
        // TC-FR04-01: Accounts A and B -> transfer 25.00 from A to B -> A falls by 25, B rises by 25, net worth unchanged
        Account accountA = createTestAccount("Source", Money.of("100.00"));
        Account accountB = createTestAccount("Destination", Money.of("50.00"));

        Money initialNetWorth = accountA.balance().add(accountB.balance());
        assertEquals(Money.of("150.00"), initialNetWorth);

        Transfer transfer = new Transfer(3L, TEST_DATE, Money.of("25.00"), "Savings Move", accountA, accountB, null);
        transfer.executeTransfer();

        assertEquals(Money.of("75.00"), accountA.balance());
        assertEquals(Money.of("75.00"), accountB.balance());

        Money finalNetWorth = accountA.balance().add(accountB.balance());
        assertEquals(initialNetWorth, finalNetWorth);

        // Net effect of both legs must sum to exactly zero
        long netEffect = transfer.effectOn(accountA) + transfer.effectOn(accountB);
        assertEquals(0L, netEffect);
    }

    @Test
    void transferRejectsSameSourceAndDestination() {
        Account account = createTestAccount("Same", Money.of("100.00"));
        assertThrows(IllegalArgumentException.class, () ->
                new Transfer(3L, TEST_DATE, Money.of("25.00"), "Self", account, account, null));
    }

    @Test
    void transferAssignsUuidLinkIdWhenOmitted() {
        Account a = createTestAccount("A", Money.of("100.00"));
        Account b = createTestAccount("B", Money.of("100.00"));
        Transfer transfer = new Transfer(3L, TEST_DATE, Money.of("25.00"), "Test", a, b, null);

        assertNotNull(transfer.transferLinkId());
        assertFalse(transfer.transferLinkId().isBlank());
        assertEquals("TRANSFER", transfer.kind());
        assertEquals(b, transfer.toAccount());
    }

    @Test
    void reverseTransferRestoresSourceAndDestination() throws InsufficientFundsException {
        Account a = createTestAccount("A", Money.of("100.00"));
        Account b = createTestAccount("B", Money.of("50.00"));
        Transfer transfer = new Transfer(3L, TEST_DATE, Money.of("25.00"), "Move", a, b, null);

        transfer.executeTransfer();
        assertEquals(Money.of("75.00"), a.balance());
        assertEquals(Money.of("75.00"), b.balance());

        transfer.reverseTransfer();
        assertEquals(Money.of("100.00"), a.balance());
        assertEquals(Money.of("50.00"), b.balance());
    }

    @Test
    void accountApplyAndReverseDelegatePolymorphically() throws InsufficientFundsException {
        Account a = createTestAccount("A", Money.of("100.00"));
        Account b = createTestAccount("B", Money.of("50.00"));
        Account c = createTestAccount("C", Money.of("50.00"));
        Transfer transfer = new Transfer(3L, TEST_DATE, Money.of("20.00"), "Polymorphic", a, b, "uuid-link");

        a.apply(transfer);
        b.apply(transfer);
        c.apply(transfer); // Unrelated account unaffected

        assertEquals(Money.of("80.00"), a.balance());
        assertEquals(Money.of("70.00"), b.balance());
        assertEquals(Money.of("50.00"), c.balance());

        a.reverse(transfer);
        b.reverse(transfer);
        c.reverse(transfer); // Unrelated account unaffected

        assertEquals(Money.of("100.00"), a.balance());
        assertEquals(Money.of("50.00"), b.balance());
    }

    @Test
    void equalsAndHashCodeContract() {
        Account a = createTestAccount("A", Money.of("100.00"));
        Account b = createTestAccount("B", Money.of("50.00"));
        Transfer t1 = new Transfer(30L, TEST_DATE, Money.of("10.00"), "T", a, b, "link-1");
        Transfer t2 = new Transfer(30L, TEST_DATE, Money.of("10.00"), "T", a, b, "link-1");
        Transfer t3 = new Transfer(31L, TEST_DATE, Money.of("10.00"), "T", a, b, "link-1");

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertNotEquals(t1, t3);
    }
}
