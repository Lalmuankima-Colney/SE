package piems.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Transaction representing a fund transfer between two accounts.
 *
 * <p>Implements Requirement FR-04 and Decision D-23. Transfers maintain source
 * and destination accounts, a shared UUID {@code transferLinkId}, and preserve net worth
 * identically across both legs (effect sums to zero).</p>
 */
public class Transfer extends Transaction {

    private final Account toAccount;
    private final String transferLinkId;

    /**
     * Constructs a Transfer transaction with full parameters.
     *
     * @param id             unique transaction ID, or null if unpersisted
     * @param date           transaction date
     * @param amount         positive transfer amount
     * @param note           optional note
     * @param fromAccount    source account debited
     * @param toAccount      destination account credited
     * @param transferLinkId UUID linking transfer legs, or null to auto-generate
     */
    public Transfer(
            Long id,
            LocalDate date,
            Money amount,
            String note,
            Account fromAccount,
            Account toAccount,
            String transferLinkId
    ) {
        super(id, date, amount, note, fromAccount, (CategoryComponent) null);
        this.toAccount = Objects.requireNonNull(toAccount, "Destination account cannot be null");
        if (fromAccount.equals(toAccount)) {
            throw new IllegalArgumentException("Cannot transfer funds to the same account");
        }
        this.transferLinkId = transferLinkId != null && !transferLinkId.isBlank()
                ? transferLinkId.trim()
                : UUID.randomUUID().toString();
    }

    /**
     * Constructs a single-leg Transfer transaction representation.
     *
     * @param id             unique transaction ID, or null if unpersisted
     * @param date           transaction date
     * @param amount         positive transfer amount
     * @param note           optional note
     * @param account        associated leg account
     * @param transferLinkId UUID linking transfer legs, or null to auto-generate
     */
    public Transfer(
            Long id,
            LocalDate date,
            Money amount,
            String note,
            Account account,
            String transferLinkId
    ) {
        super(id, date, amount, note, account, (CategoryComponent) null);
        this.toAccount = null;
        this.transferLinkId = transferLinkId != null && !transferLinkId.isBlank()
                ? transferLinkId.trim()
                : UUID.randomUUID().toString();
    }

    /**
     * Returns the destination account.
     *
     * @return destination Account
     */
    public Account toAccount() {
        return toAccount;
    }

    /**
     * Returns the shared UUID linking both legs of this transfer.
     *
     * @return non-empty UUID string
     */
    public String transferLinkId() {
        return transferLinkId;
    }

    @Override
    public String kind() {
        return "TRANSFER";
    }

    @Override
    public long effectOn(Account target) {
        if (target == null) {
            return 0L;
        }
        if (target.equals(account())) {
            return -amount().minorUnits();
        }
        if (target.equals(toAccount)) {
            return amount().minorUnits();
        }
        return 0L;
    }

    @Override
    public void applyTo(Account target) throws InsufficientFundsException {
        if (target != null) {
            if (target.equals(account())) {
                target.debit(amount());
            } else if (target.equals(toAccount)) {
                target.credit(amount());
            }
        }
    }

    @Override
    public void reverseOn(Account target) throws InsufficientFundsException {
        if (target != null) {
            if (target.equals(account())) {
                target.credit(amount());
            } else if (target.equals(toAccount)) {
                target.debit(amount());
            }
        }
    }

    /**
     * Executes the transfer across both accounts atomically.
     *
     * @throws InsufficientFundsException if the source account cannot accept the debit
     */
    public void executeTransfer() throws InsufficientFundsException {
        account().debit(amount());
        toAccount.credit(amount());
    }

    /**
     * Reverses the transfer across both accounts atomically.
     *
     * @throws InsufficientFundsException if the destination account cannot accept the debit
     */
    public void reverseTransfer() throws InsufficientFundsException {
        toAccount.debit(amount());
        account().credit(amount());
    }
}
