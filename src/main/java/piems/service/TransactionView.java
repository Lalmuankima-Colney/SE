package piems.service;

import java.time.LocalDate;
import java.util.Objects;
import piems.domain.Money;

/**
 * Immutable view of a transaction exposed to client and presentation layers.
 *
 * <p>Shields callers from underlying persistence models in accordance with Decision D-05.</p>
 *
 * @param id             transaction identifier
 * @param kind           transaction kind ("INCOME", "EXPENSE", "TRANSFER")
 * @param date           transaction date
 * @param amount         monetary amount
 * @param note           optional note
 * @param accountId      associated account ID
 * @param transferLinkId optional transfer link identifier
 * @param categoryId     optional category identifier
 */
public record TransactionView(
        Long id,
        String kind,
        LocalDate date,
        Money amount,
        String note,
        long accountId,
        String transferLinkId,
        Long categoryId
) {

    /**
     * Compact constructor validating non-null requirements for core fields.
     */
    public TransactionView {
        Objects.requireNonNull(kind, "kind cannot be null");
        Objects.requireNonNull(date, "date cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
    }
}
