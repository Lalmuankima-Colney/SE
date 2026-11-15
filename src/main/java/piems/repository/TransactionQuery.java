package piems.repository;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Filter criteria specification for querying transaction records (FR-08).
 *
 * <p>Supports filtering by date range [startDate, endDate], account identifier,
 * category identifier, and substring search on note text. All specified criteria
 * are combined with logical AND.</p>
 *
 * @param startDate  inclusive start date, or null for unbounded start
 * @param endDate    inclusive end date, or null for unbounded end
 * @param accountId  account identifier to match, or null for any account
 * @param categoryId category identifier to match, or null for any category
 * @param text       substring to search in note text (case-insensitive), or null
 */
public record TransactionQuery(
        LocalDate startDate,
        LocalDate endDate,
        Long accountId,
        Long categoryId,
        String text
) {

    /**
     * Compact constructor validating temporal ordering and normalizing note search text.
     */
    public TransactionQuery {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }
        text = (text != null && !text.isBlank()) ? text.trim() : null;
    }

    /**
     * Creates an unconstrained query matching all transactions.
     *
     * @return empty TransactionQuery matching all records
     */
    public static TransactionQuery all() {
        return new TransactionQuery(null, null, null, null, null);
    }

    /**
     * Creates a builder for fluent query construction.
     *
     * @return new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Evaluates whether a given transaction record matches all specified criteria.
     *
     * @param record transaction record to test
     * @return true if all non-null criteria match with logical AND, false otherwise
     */
    public boolean matches(TransactionRecord record) {
        Objects.requireNonNull(record, "record cannot be null");
        if (startDate != null && record.date().isBefore(startDate)) {
            return false;
        }
        if (endDate != null && record.date().isAfter(endDate)) {
            return false;
        }
        if (accountId != null && record.accountId() != accountId) {
            return false;
        }
        if (categoryId != null && (record.categoryId() == null || !record.categoryId().equals(categoryId))) {
            return false;
        }
        if (text != null) {
            if (record.note() == null || !record.note().toLowerCase().contains(text.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fluent builder for {@link TransactionQuery}.
     */
    public static class Builder {
        private LocalDate startDate;
        private LocalDate endDate;
        private Long accountId;
        private Long categoryId;
        private String text;

        /**
         * Sets inclusive start date.
         *
         * @param startDate start date
         * @return this builder
         */
        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        /**
         * Sets inclusive end date.
         *
         * @param endDate end date
         * @return this builder
         */
        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        /**
         * Sets inclusive date range.
         *
         * @param start start date
         * @param end   end date
         * @return this builder
         */
        public Builder dateRange(LocalDate start, LocalDate end) {
            this.startDate = start;
            this.endDate = end;
            return this;
        }

        /**
         * Sets account identifier.
         *
         * @param accountId account ID
         * @return this builder
         */
        public Builder accountId(Long accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets category identifier.
         *
         * @param categoryId category ID
         * @return this builder
         */
        public Builder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        /**
         * Sets search text for note matching.
         *
         * @param text substring to match
         * @return this builder
         */
        public Builder text(String text) {
            this.text = text;
            return this;
        }

        /**
         * Builds the configured TransactionQuery instance.
         *
         * @return immutable TransactionQuery
         */
        public TransactionQuery build() {
            return new TransactionQuery(startDate, endDate, accountId, categoryId, text);
        }
    }
}
