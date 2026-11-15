package piems.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result metrics and diagnostic report produced by a CSV import operation (FR-17, D-30).
 *
 * @param importedCount  count of newly imported valid transactions
 * @param duplicateCount count of detected duplicate transactions skipped
 * @param skippedRows    list of malformed or rejected records with diagnostic details
 */
public record ImportResult(
        int importedCount,
        int duplicateCount,
        List<SkippedRow> skippedRows
) {

    /**
     * Compact constructor guaranteeing an unmodifiable list of skipped rows.
     */
    public ImportResult {
        Objects.requireNonNull(skippedRows, "skippedRows cannot be null");
        skippedRows = Collections.unmodifiableList(skippedRows);
    }

    /**
     * Total number of records evaluated during the import operation.
     *
     * @return sum of imported, duplicate, and skipped record counts
     */
    public int totalProcessed() {
        return importedCount + duplicateCount + skippedRows.size();
    }

    /**
     * Diagnostic record describing a rejected or skipped CSV row.
     *
     * @param lineNumber 1-based source line number in the CSV document
     * @param rawContent raw text content of the row
     * @param reason     human-readable explanation for why the row was skipped
     */
    public record SkippedRow(int lineNumber, String rawContent, String reason) {

        /**
         * Compact constructor validating required diagnostic fields.
         */
        public SkippedRow {
            Objects.requireNonNull(rawContent, "rawContent cannot be null");
            Objects.requireNonNull(reason, "reason cannot be null");
        }
    }
}
