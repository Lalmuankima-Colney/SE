package piems.report;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Request parameter object for report generation (Decision D-10).
 *
 * <p>Encapsulates target reporting periods including single-month targets and
 * multi-month continuous ranges. Validates date bounds upon construction.</p>
 */
public final class ReportRequest {

    private final YearMonth startMonth;
    private final YearMonth endMonth;

    /**
     * Constructs a single-month report request.
     *
     * @param month target calendar month
     */
    public ReportRequest(YearMonth month) {
        this(month, month);
    }

    /**
     * Constructs a multi-month range report request.
     *
     * @param startMonth beginning month of the reporting range (inclusive)
     * @param endMonth   ending month of the reporting range (inclusive)
     */
    public ReportRequest(YearMonth startMonth, YearMonth endMonth) {
        Objects.requireNonNull(startMonth, "Start month cannot be null");
        Objects.requireNonNull(endMonth, "End month cannot be null");
        if (startMonth.isAfter(endMonth)) {
            throw new IllegalArgumentException(
                    "Start month (" + startMonth + ") cannot be after end month (" + endMonth + ")");
        }
        this.startMonth = startMonth;
        this.endMonth = endMonth;
    }

    /**
     * Returns the target single month (equivalent to startMonth).
     *
     * @return target month
     */
    public YearMonth month() {
        return startMonth;
    }

    /**
     * Returns the start month of the reporting range.
     *
     * @return start month
     */
    public YearMonth startMonth() {
        return startMonth;
    }

    /**
     * Returns the end month of the reporting range.
     *
     * @return end month
     */
    public YearMonth endMonth() {
        return endMonth;
    }

    /**
     * Tests whether a given transaction date falls within the single target month.
     *
     * @param date transaction date to test
     * @return true if date occurs in target month
     */
    public boolean matchesMonth(LocalDate date) {
        if (date == null) {
            return false;
        }
        return YearMonth.from(date).equals(startMonth);
    }

    /**
     * Tests whether a given transaction date falls within the range [startMonth, endMonth].
     *
     * @param date transaction date to test
     * @return true if date occurs within range
     */
    public boolean matchesRange(LocalDate date) {
        if (date == null) {
            return false;
        }
        YearMonth ym = YearMonth.from(date);
        return !ym.isBefore(startMonth) && !ym.isAfter(endMonth);
    }
}
