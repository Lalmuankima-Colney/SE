package piems.report;

import java.time.LocalDate;
import java.util.Objects;
import piems.domain.Money;

/**
 * Immutable row representation of a transaction for export and tabular display.
 *
 * <p>Represents transactions in the unified 7-column schema specified in SRS Section 3.3
 * and Decisions D-23 and D-30: {@code date,kind,account,to_account,category,amount,note}.</p>
 *
 * @param date      transaction date
 * @param kind      transaction kind ("INCOME", "EXPENSE", or "TRANSFER")
 * @param account   source or primary account display name
 * @param toAccount destination account name for transfers, or empty string
 * @param category  associated category name, or empty string
 * @param amount    monetary transaction amount
 * @param note      optional note or description
 */
public record TransactionExportRow(
        LocalDate date,
        String kind,
        String account,
        String toAccount,
        String category,
        Money amount,
        String note
) {

    /**
     * Compact constructor normalizing null strings to empty values.
     */
    public TransactionExportRow {
        Objects.requireNonNull(date, "date cannot be null");
        Objects.requireNonNull(kind, "kind cannot be null");
        Objects.requireNonNull(account, "account cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        toAccount = (toAccount == null) ? "" : toAccount.trim();
        category = (category == null) ? "" : category.trim();
        note = (note == null) ? "" : note.trim();
    }
}
