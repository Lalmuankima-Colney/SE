package piems.service;

import java.time.LocalDate;
import java.util.Objects;
import piems.domain.Money;

/**
 * Immutable view of a transaction exposed to client and presentation layers.
 *
 * <p>Shields callers from underlying persistence models in accordance with Decision D-05, D-48.</p>
 *
 * @param id             transaction identifier
 * @param kind           transaction kind ("INCOME", "EXPENSE", "TRANSFER")
 * @param date           transaction date
 * @param amount         monetary amount
 * @param note           optional note
 * @param accountId      associated account ID
 * @param transferLinkId optional transfer link identifier
 * @param categoryId     optional category identifier
 * @param accountName    resolved account display name (Decision D-48)
 * @param categoryName   resolved category display name or null (Decision D-48)
 */
public record TransactionView(
        Long id,
        String kind,
        LocalDate date,
        Money amount,
        String note,
        long accountId,
        String transferLinkId,
        Long categoryId,
        String accountName,
        String categoryName
) {

    /**
     * Backward-compatible convenience constructor defaulting account and category names to null.
     *
     * @param id             transaction identifier
     * @param kind           transaction kind
     * @param date           transaction date
     * @param amount         monetary amount
     * @param note           optional note
     * @param accountId      associated account ID
     * @param transferLinkId optional transfer link identifier
     * @param categoryId     optional category identifier
     */
    public TransactionView(
            Long id,
            String kind,
            LocalDate date,
            Money amount,
            String note,
            long accountId,
            String transferLinkId,
            Long categoryId
    ) {
        this(id, kind, date, amount, note, accountId, transferLinkId, categoryId, null, null);
    }

    /**
     * Compact constructor validating non-null requirements for core fields.
     */
    public TransactionView {
        Objects.requireNonNull(kind, "kind cannot be null");
        Objects.requireNonNull(date, "date cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
    }
}
