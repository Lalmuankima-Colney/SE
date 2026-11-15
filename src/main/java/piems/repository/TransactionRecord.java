package piems.repository;

import java.time.LocalDate;
import java.util.Objects;
import piems.domain.Money;

/**
 * Immutable record representing a transaction row in the database.
 *
 * @param id             unique transaction identifier, null if not yet persisted
 * @param kind           transaction kind ("INCOME", "EXPENSE", "TRANSFER")
 * @param date           transaction date
 * @param amount         monetary amount stored as integer minor units
 * @param note           optional note or description
 * @param accountId      identifier of the associated account
 * @param transferLinkId UUID linking both legs of a transfer, null for regular transactions
 * @param categoryId     identifier of the associated category, null for transfers
 * @param ruleId         identifier of the recurring rule that generated this, if any
 */
public record TransactionRecord(
        Long id,
        String kind,
        LocalDate date,
        Money amount,
        String note,
        long accountId,
        String transferLinkId,
        Long categoryId,
        Long ruleId
) {

    /**
     * Compact constructor validating non-null requirements for core fields.
     */
    public TransactionRecord {
        Objects.requireNonNull(kind, "kind cannot be null");
        Objects.requireNonNull(date, "date cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
    }
}
